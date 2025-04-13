package org.tinkerhub.offgo.mysql_service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.tinkerhub.offgo.Repository.UserRepository;
import org.tinkerhub.offgo.entity.User;

import java.util.List;
import java.util.Optional;

@Service

public class User_service {

    @Autowired
    private UserRepository userRepository;

    public User saveUser(User user) {
        return userRepository.save(user);
    }

    public int FindMaxID() {
        if (userRepository.findAll().isEmpty()) {
            return 0;
        }

        return userRepository.findMaxID();
    }
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public User getUserById(Integer id) {
        return userRepository.findByID(id);
    }

    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }

    public User updateUser(Long id, User updatedUser) {
        return userRepository.findById(id)
                .map(user -> {
                    user.setName(updatedUser.getName());

                    return userRepository.save(user);
                })
                .orElse(null);
    }
    public User getUserByName(String name) {
        return userRepository.findByName(name);
    }
    public void deleteall() {
        userRepository.deleteAll();
    }
}
