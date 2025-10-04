package ru.itmo.infosec.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.itmo.infosec.dto.AuthResponseDto;
import ru.itmo.infosec.dto.LoginRequestDto;
import ru.itmo.infosec.dto.UserDto;
import ru.itmo.infosec.entity.User;
import ru.itmo.infosec.repo.UserRepository;
import ru.itmo.infosec.util.EntityMapper;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EntityMapper entityMapper;
    private final JwtService jwtService;

    @Transactional
    public AuthResponseDto login(LoginRequestDto loginRequestDto) {
        Optional<User> existingUser = userRepository.findByUsername(loginRequestDto.getUsername());

        if (existingUser.isPresent()) {
            User user = existingUser.get();

            if (!passwordEncoder.matches(loginRequestDto.getPassword(), user.getPassword())) {
                passwordEncoder.encode("dummy");
                throw new BadCredentialsException("Invalid username or password");
            }

            UserDto userDto = entityMapper.toUserDto(user);
            String token = jwtService.generateToken(userDto.getUsername());
            long expirationTime = System.currentTimeMillis() + jwtService.getJwtExpiration();

            return new AuthResponseDto(token, expirationTime, userDto);

        } else {
            if (!isPasswordStrong(loginRequestDto.getPassword())) {
                throw new IllegalArgumentException(
                        "Password must contain at least 8 characters, including uppercase, lowercase, number and special character");
            }

            LoginRequestDto safeDto = new LoginRequestDto(loginRequestDto.getUsername(), loginRequestDto.getPassword());

            User user = entityMapper.toUserEntity(safeDto, passwordEncoder);
            User savedUser = userRepository.save(user);
            UserDto registeredUser = entityMapper.toUserDto(savedUser);

            String token = jwtService.generateToken(registeredUser.getUsername());
            long expirationTime = System.currentTimeMillis() + jwtService.getJwtExpiration();

            return new AuthResponseDto(token, expirationTime, registeredUser);
        }
    }

    private boolean isPasswordStrong(String password) {
        if (password == null || password.length() < 8) {
            return false;
        }

        boolean hasUpper = password.chars().anyMatch(Character::isUpperCase);
        boolean hasLower = password.chars().anyMatch(Character::isLowerCase);
        boolean hasDigit = password.chars().anyMatch(Character::isDigit);
        boolean hasSpecial = password.chars().anyMatch(ch -> !Character.isLetterOrDigit(ch));

        return hasUpper && hasLower && hasDigit && hasSpecial;
    }
}