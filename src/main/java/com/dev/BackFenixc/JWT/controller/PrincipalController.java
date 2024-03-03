package com.dev.BackFenixc.JWT.controller;

import com.dev.BackFenixc.JWT.controller.request.CreateUserDTO;
import com.dev.BackFenixc.JWT.models.ERol;
import com.dev.BackFenixc.JWT.models.RolEntity;
import com.dev.BackFenixc.JWT.models.UserEntity;
import com.dev.BackFenixc.JWT.repositories.UserRepository;
import jakarta.validation.Valid;
import org.apache.catalina.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Set;
import java.util.stream.Collectors;

@RestController
public class PrincipalController {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @GetMapping("/hello")
    public String hello(){
        return "hello not secured ";
    }
    @GetMapping("/helloSecured")
    public String helloSecured(){
        return "hello secured ";
    }
    @PostMapping ("/create")
    public ResponseEntity<?> createUser (@Valid @RequestBody CreateUserDTO createUserDTO){
        Set<RolEntity> roles= createUserDTO.getRol().stream()
                .map(rol->RolEntity.builder()
                        .rol(ERol.valueOf(rol))
                        .build())
                .collect(Collectors.toSet());

        UserEntity userEntity = UserEntity.builder()
                .username(createUserDTO.getUsername())
                .email(createUserDTO.getEmail())
                .password(passwordEncoder.encode(createUserDTO.getPassword()))
                .rol(roles)//quemado cliente
                .build();
        userRepository.save(userEntity);
        return  ResponseEntity.ok(userEntity);


    }

    @DeleteMapping("/deleteUser")
    public String deleteUser (@RequestParam String id){
        userRepository.deleteById(Long.parseLong(id));
        return "Se ha borrado exitosamende".concat(id);
    }

}
