/*
 * Copyright 2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.angrydwarfs.framework.controllers;

import com.angrydwarfs.framework.models.Enums.EMainRole;
import com.angrydwarfs.framework.models.MainRole;
import com.angrydwarfs.framework.models.Token;
import com.angrydwarfs.framework.models.User;
import com.angrydwarfs.framework.payload.request.SignupRequest;
import com.angrydwarfs.framework.payload.response.MessageResponse;
import com.angrydwarfs.framework.repository.MainRoleRepository;
import com.angrydwarfs.framework.repository.TokenRepository;
import com.angrydwarfs.framework.repository.UserRepository;
import com.angrydwarfs.framework.security.jwt.UserUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Контроллер работы с пользователями.
 * @version 0.001
 * @author habatoo
 */
@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/auth/users")
public class UserController {
    @Value("${dwarfsframework.app.remoteAddr}")
    private String remoteAddr;

    private final UserRepository userRepository;
    private final TokenRepository tokenRepository;

    @Autowired
    public UserController(UserRepository userRepository,
                           TokenRepository tokenRepository
    ) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
    }

    @Autowired
    MainRoleRepository roleRepository;

    @Autowired
    PasswordEncoder encoder;

    @Autowired
    UserUtils userUtils;

    /**
     * @method userList - при http GET запросе по адресу .../api/auth/users
     * @return {@code List<user>} - список всех пользователей с полными данными пользователей.
     * @see User
     * @see com.angrydwarfs.framework.models.MainRole
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseBody
    public ResponseEntity<?> userList() {
        List<Object> usersReturn = new ArrayList<>();
        List<User> usersCurrent = userRepository.findAll();
        for(User user: usersCurrent) {
            Map<String, Object> temp = new HashMap<String, Object>();
            //temp.put("roles", user.getRoles());
            temp.put("creationDate", user.getCreationDate());
            temp.put("userEmail", user.getUserEmail());
            temp.put("userName", user.getUserName());
            temp.put("id", user.getId());
            usersReturn.add(temp);
        }

        return ResponseEntity.ok(usersReturn);
    }

    /**
     * @method getUserInfo - при http GET запросе по адресу .../api/auth/users/getUserInfo
     * @param authentication - данные по текущему аутентифицированному пользователю
     * возвращает данные
     * @return {@code userRepository} - полные данные пользователя - user.userName, user.balance, user.roles
     * @see UserRepository
     */
//    @GetMapping("/getUserInfo")
//    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
//    @ResponseBody
//    public ResponseEntity<?>  getUserInfo(Authentication authentication) {
//        User user = userRepository.findByUserName(authentication.getName()).get();
//        return ResponseEntity.ok(
//                user.getUserName(),
//                user.getUserEmail(),
//                user.getCreationDate()
//        );
//    }

    /**
     * @method registerUser - при http POST запросе по адресу .../api/auth/users/addUser
     * @param signUpRequest - входные данные по текущему аутентифицированному пользователю
     * возвращает данные
     * @return {@code ResponseEntity.ok - User registered successfully!} - ок при успешной регистрации.
     * @return {@code ResponseEntity.badRequest - Error: Role is not found.} - ошибка при указании неправильной роли.
     * @return {@code ResponseEntity.badRequest - Error: Username is already taken!} - ошибка при дублировании username при регистрации.
     * @return {@code ResponseEntity.badRequest - Error: Email is already in use!} - ошибка при дублировании email при регистрации.
     * @see ResponseEntity
     * @see SignupRequest
     * метод доступен только для пользователей с ролью ADMIN
     */
    @PostMapping("/addUser")
    public ResponseEntity<?> registerUser(@Valid @RequestBody SignupRequest signUpRequest, HttpServletRequest request) {
        if (!(remoteAddr.equals(request.getRemoteAddr()) || "127.0.0.1".equals(request.getRemoteAddr()) | "localhost".equals(request.getRemoteAddr()))) {
            return ResponseEntity
                    .badRequest()
                    .body(new MessageResponse("Not support IP!"));
        }

        if (userRepository.existsByUserName(
                signUpRequest.getUserName()
        )) {
            return ResponseEntity
                    .badRequest()
                    .body(new MessageResponse("Error: Username is already taken!"));
        }

        if (userRepository.existsByUserEmail(signUpRequest.getEmail())) {
            return ResponseEntity
                    .badRequest()
                    .body(new MessageResponse("Error: Email is already in use!"));
        }

        // Create new user's account
        User user = new User(
                signUpRequest.getUserName(),
                signUpRequest.getEmail(),
                encoder.encode(signUpRequest.getPassword())
        );

        Set<String> strRoles = signUpRequest.getRole();
        Set<MainRole> roles = new HashSet<>();

        if (strRoles == null) {
            MainRole userRole = roleRepository.findByMainRoleName(EMainRole.ROLE_USER)
                    .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
            roles.add(userRole);
        } else {
            strRoles.forEach(role -> {
                switch (role) {
                    case "admin":
                        MainRole adminRole = roleRepository.findByMainRoleName(EMainRole.ROLE_ADMINISTRATOR)
                                .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
                        roles.add(adminRole);
                        break;

                    case "mod":
                        MainRole modRole = roleRepository.findByMainRoleName(EMainRole.ROLE_MODERATOR)
                                .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
                        roles.add(modRole);
                        break;

                    default:
                        MainRole userRole = roleRepository.findByMainRoleName(EMainRole.ROLE_USER)
                                .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
                        roles.add(userRole);
                }
            });
        }

        user.setMainRoles(roles);
        user.setCreationDate(LocalDateTime.now());
        userRepository.save(user);

        return ResponseEntity.ok(new MessageResponse("User registered successfully!"));
    }

    /**
     * @method changeUser - при http PUT запросе по адресу .../api/auth/users/{id}
     * {id} - входные данные - id пользователя, данные которого редактируются, id не редактируетс
     * возвращает данные
     * @return - измененные данные пользовалеля, id изменению не подлежит.
     * @param userFromDb - данные пользователя отредактированные из формы
     * @param user - текущие данные пользователя
     * @see UserRepository
     */
    @PutMapping("{id}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<?> changeUser(
            @PathVariable("id") User userFromDb,
            @RequestBody User user,
            Authentication authentication) {

        userFromDb = userRepository.findById(userFromDb.getId()).get();
        // check ID current user = ID edit user
        if(!(userFromDb.getId() == userRepository.findByUserName(authentication.getName()).get().getId())) {
            // admin check
            if(userRepository.findByUserName(authentication.getName()).get().getMainRoles().size() == 3) {
                //BeanUtils.copyProperties(user, userRepository.findById(userFromDb.getId()).get(), "id");
                return userUtils.checkUserNameAndEmail(user, userFromDb);
            }
            return ResponseEntity
                    .badRequest()
                    .body(new MessageResponse("You can edit only yourself data."));
        } else {
            //BeanUtils.copyProperties(user, userRepository.findById(userFromDb.getId()).get(), "id");
            return userUtils.checkUserNameAndEmail(user, userFromDb);
        }

    }

    /**
     * @method deleteUser - при http DELETE запросе по адресу .../api/auth/users/{id}
     * {id} - входные данные - id пользователя, данные которого удаляются.
     * @param user - обьект пользователя для удаления.
     * @see UserRepository
     */
    @DeleteMapping("{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?>  deleteUser(@PathVariable("id") User user) {
        try {
            userRepository.delete(user);
            return ResponseEntity.ok(new MessageResponse("User was deleted successfully!"));
        } catch (Exception e) {
            return ResponseEntity
                    .badRequest()
                    .body(new MessageResponse("Error: User was not deleted!"));
        }
    }

    /**
     * @method clearTokens - при http DELETE запросе по адресу .../api/auth/users/tokens - очищает базу от токенов с истекшим сроком
     * @return {@code ResponseEntity.badRequest - All tokens have valid expiry date!} - если все токены имеют не истекший срок действия.
     * @return {@code ResponseEntity.badRequest - Error: Can't read token data!} - ошибка при запросе к таблице token.
     * @return {@code ResponseEntity.ok - Tokens with expiry date was deleted successfully!} - при успешном удалении токенов с истекшим сроком действия.
     */
    @DeleteMapping("/tokens")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?>  clearTokens() {

        try {
            List<Token> tokens = tokenRepository.findByExpiryDateBefore(LocalDateTime.now());
            if(tokens.isEmpty()) {
                return ResponseEntity
                        .badRequest()
                        .body(new MessageResponse("All tokens have valid expiry date!"));
            }
            else {
                for (Token token : tokens) {
                    try { tokenRepository.deleteById(token.getId()); } catch (Exception e) {
                        return ResponseEntity
                                .badRequest()
                                .body(new MessageResponse("Error: Can't delete token!"));
                    }
                }
                return ResponseEntity.ok(new MessageResponse("Tokens with expiry date was deleted successfully!"));
            }

        } catch (Exception e) {
            return ResponseEntity
                    .badRequest()
                    .body(new MessageResponse("Error: Can't read token data!"));
        }

    }
}
