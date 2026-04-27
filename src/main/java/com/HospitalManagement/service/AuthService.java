package com.HospitalManagement.service;

import com.HospitalManagement.entity.User;
import com.HospitalManagement.enums.Roles;
import com.HospitalManagement.repository.UserRepository;
import com.HospitalManagement.requestdto.AuthenticationRequest;
import com.HospitalManagement.requestdto.RegisterRequest;
import com.HospitalManagement.responsedto.AuthenticationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthenticationResponse register(RegisterRequest request) {
        if (repository.existsByEmail(request.getEmail())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "User already exists with email: " + request.getEmail());
        }

        Roles role = parsePatientRole(request.getRole());
        var user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .phone(request.getPhone())
                .role(role)
                .status("ACTIVE")
                .build();
        repository.save(user);

        return AuthenticationResponse.builder()
                .message("User registered successfully")
                .build();
    }

    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );
        var user = repository.findByEmail(request.getEmail())
                .orElseThrow();
        var jwtToken = jwtService.generateToken(user);
        return AuthenticationResponse.builder()
                .token(jwtToken)
                .message("User authenticated successfully")
                .build();
    }

    private Roles parsePatientRole(String roleValue) {
        if (roleValue == null || roleValue.isBlank()) {
            return Roles.PATIENT;
        }

        try {
            Roles role = Roles.valueOf(roleValue.toUpperCase());
            if (role != Roles.PATIENT) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Public registration is only available for PATIENT role");
            }
            return role;
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid role: " + roleValue);
        }
    }
}
