package com.dev.BackFenixc.JWT.controller;

import com.dev.BackFenixc.JWT.controller.request.CreateUserDTO;
import com.dev.BackFenixc.JWT.models.ERol;
import com.dev.BackFenixc.JWT.models.RolEntity;
import com.dev.BackFenixc.JWT.models.UserEntity;
import com.dev.BackFenixc.JWT.repositories.UserRepository;
import com.dev.BackFenixc.JWT.security.util.EmailUtil;
import com.dev.BackFenixc.JWT.security.util.OtpUtil;
import com.dev.BackFenixc.JWT.security.util.ResetUtil;
import jakarta.validation.Valid;
import org.apache.catalina.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import javax.mail.MessagingException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
public class PrincipalController {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private OtpUtil otpUtil;

    @Autowired
    private EmailUtil emailUtil;
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
        String otp = otpUtil.generateOtp();
        try{
            emailUtil.sendOtpEmail(createUserDTO.getEmail(), otp);
        } catch (jakarta.mail.MessagingException e) {
            throw new RuntimeException("No se genero codigo de activacion"+e);
        }

        Set<RolEntity> roles= createUserDTO.getRol().stream()
                .map(rol->RolEntity.builder()
                        .rol(ERol.valueOf(rol))
                        .build())
                .collect(Collectors.toSet());

        UserEntity userEntity = UserEntity.builder()
                .username(createUserDTO.getUsername())
                .email(createUserDTO.getEmail())
                .password(passwordEncoder.encode(createUserDTO.getPassword()))
                .otp(otp)
                .otpGeneratedTime(LocalDateTime.now())
                .rol(roles)//quemado cliente
                .build();
        userRepository.save(userEntity);
        return  ResponseEntity.ok(userEntity);


    }

    @PutMapping("/verify-account")
    public String verifyAccount (String email, String otp){
        UserEntity userEntity = userRepository.findByEmail(email).orElseThrow(
                ()-> new RuntimeException("Este correo "+ email+ " no se encontro")
        );
        if (userEntity.getOtp().equals(otp) && Duration.between(userEntity.getOtpGeneratedTime(),LocalDateTime.now()).getSeconds()<(1000*60)){
            userEntity.setActive(true);
            userRepository.save(userEntity);
            return "Correo confirmado";
        }
        return "Tiempo excedido del codigo, generelo otra vez ";
    }

    @GetMapping("/reset-password")
    public String resetPass (@Valid @RequestBody ResetUtil resetUtil){

        try{
            emailUtil.sendPassword(resetUtil.getEmail(), resetUtil.getPassword());
        } catch (jakarta.mail.MessagingException e) {
            throw new RuntimeException("No se genero codigo de restablecimiento de contraseña"+e);
        }

        return "correo enviado";
    }

    @PutMapping("/reset-password")
    public String resetPassword (String email, @RequestBody ResetUtil resetUtil){
        UserEntity userEntity = userRepository.findByEmail(email).orElseThrow(
                ()-> new RuntimeException("Este usuario "+ email+ " no se encontro")
        );
        if (userEntity.isActive()){
            userEntity.setPassword(resetUtil.getPassword());
            userRepository.save(userEntity);
            return "Contraseña actualizada correctamente";
        }
        return "Tiempo excedido del codigo, generelo otra vez ";
    }

    @DeleteMapping("/deleteUser")
    public String deleteUser (@RequestParam String id){
        userRepository.deleteById(Long.parseLong(id));
        return "Se ha borrado exitosamende".concat(id);
    }

}
