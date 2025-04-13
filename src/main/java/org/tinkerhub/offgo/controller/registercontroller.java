package org.tinkerhub.offgo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.tinkerhub.offgo.entity.User;
import org.tinkerhub.offgo.Repository.UserRepository;
import org.tinkerhub.offgo.mysql_service.User_service;

import static java.sql.JDBCType.NULL;

@RestController
public class registercontroller {
    @Autowired
    private User_service user_service;
    @RequestMapping("/sign_in_id_{ID}_{password}")
    public boolean sign_in_ID(String ID, String password) {
        User sign_id=user_service.getUserById(Integer.parseInt(ID));
        if (sign_id!=null) {
            return sign_id.checkPassword(password);
        }
        else {
            return false;
        }
    }
    @RequestMapping("/sign_in_name/{username}/{password}")
    public boolean sign_in_username(@PathVariable String username,@PathVariable String password) {
        User sign_name=user_service.getUserByName(username);
        if (sign_name!=null) {
            return sign_name.checkPassword(password);
        }
        else {
            return false;
        }
    }
    @RequestMapping("/sign_up/{username}/{password}")
    public int createUser( @PathVariable String username,@PathVariable String password) {
        if (user_service.getUserByName(username) != null) {
            return 0;
        }
        int ID=user_service.FindMaxID()+1;
        User savedUser =new User(username,password,ID);
        user_service.saveUser(savedUser);
        return ID;
    }
    @RequestMapping("/delete_all")
    public boolean deleteAll() {
        user_service.deleteall();
        return true;
    }
}
