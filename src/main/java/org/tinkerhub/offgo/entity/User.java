/*
 * Copyright 2013-2018 the original author or authors.
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

package org.tinkerhub.offgo.entity;

import jakarta.persistence.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tinkerhub.offgo.Repository.UserRepository;
import org.tinkerhub.offgo.mysql_service.User_service;

/**
 * @author <a href="mailto:chenxilzx1@gmail.com">theonefx</a>
 */
@Entity
@Table(name = "users")
public class User {
    @Column(name = "name")
    private String username;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int ID;
    @Column(name = "password")
    private String password;

    public User() {

    }

    public void setPassword(String password) {
        this.password = password;
    }
    public boolean checkPassword(String password) {
        return password.equals(this.password);
    }
    public boolean changePassword(String oldPassword, String newPassword) {
        if(checkPassword(oldPassword)) {
            this.password = newPassword;
            return true;
        }
        else {
            return false;
        }
    }
    public String getName() {
        return username;
    }

    public void setName(String name) {
        this.username = name;
    }

    public Integer getID() {
        return ID;
    }

    public User(String name, String password,int ID) {
        this.username = name;
        this.password = password;
        this.ID = ID;
    }
}
