package org.tinkerhub.offgo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.tinkerhub.offgo.entity.User;
import org.tinkerhub.offgo.service.UserService;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
public class registercontroller {
    private static final Logger logger = LoggerFactory.getLogger(registercontroller.class);
    
    @Autowired
    private UserService userService;
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z0-9]{6,10}$");
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^[A-Za-z0-9]{6,15}$");
    @RequestMapping("/sign_in_id_{ID}_{password}")
    public boolean sign_in_ID(String ID, String password) {
        User sign_id=userService.getUserById(Integer.parseInt(ID));
        if (sign_id!=null) {
            return sign_id.checkPassword(password);
        }
        return false;
    }
    @RequestMapping("/sign_in_name/{username}/{password}")
    public boolean sign_in_username(@PathVariable String username, @PathVariable String password) {
        System.out.println("Login attempt - Username: " + username);
        
        if ("admin".equals(username)) {
            User admin = userService.getUserByName(username);
            System.out.println("Admin user found: " + (admin != null));
            if (admin != null) {
                boolean passwordMatch = admin.checkPassword(password);
                System.out.println("Password match: " + passwordMatch);
                return passwordMatch;
            }
            return false;
        }
        
        if (!USERNAME_PATTERN.matcher(username).matches() || !PASSWORD_PATTERN.matcher(password).matches()) {
            return false;
        }
        
        User sign_name = userService.getUserByName(username);
        return sign_name != null && sign_name.checkPassword(password);
    }
    @RequestMapping("/sign_up/{username}/{password}")
    public int createUser(@PathVariable String username, @PathVariable String password) {
        logger.info("Attempting to create new user: username={}", username);
        
        try {
            // 检查用户名是否为admin
        if ("admin".equals(username)) {
                logger.warn("Attempt to create user with reserved username 'admin'");
                return -1;
            }

            // 验证用户名和密码格式
            if (!USERNAME_PATTERN.matcher(username).matches()) {
                logger.warn("Invalid username format: {}", username);
                return -1;
            }
            if (!PASSWORD_PATTERN.matcher(password).matches()) {
                logger.warn("Invalid password format for user: {}", username);
                return -1;
            }

            // 检查用户名是否已存在
            if (userService.getUserByName(username) != null) {
                logger.warn("Username already exists: {}", username);
                return 0;
            }

            // 创建新用户
            int ID = userService.FindMaxID() + 1;
            User newUser = new User(username, password, ID);
            User savedUser = userService.saveUser(newUser);
            
            if (savedUser != null && savedUser.getID() > 0) {
                logger.info("Successfully created new user: id={}, username={}", savedUser.getID(), savedUser.getName());
                return savedUser.getID();
            } else {
                logger.error("Failed to save new user: username={}", username);
            return -1;
        }
        } catch (Exception e) {
            logger.error("Error creating user: username={}, error={}", username, e.getMessage(), e);
            return -1;
        }
    }
    @RequestMapping("/get_user_id/{username}")
    public int getUserId(@PathVariable String username) {
        User user = userService.getUserByName(username);
        return user != null ? user.getID() : -1;
    }
    @RequestMapping("/get_user_info/{username}")
    public User getUserInfo(@PathVariable String username) {
        return userService.getUserByName(username);
    }
}
