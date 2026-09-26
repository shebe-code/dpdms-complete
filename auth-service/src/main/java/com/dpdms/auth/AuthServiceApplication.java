package com.dpdms.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.persistence.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.http.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;

@SpringBootApplication
public class AuthServiceApplication {
    public static void main(String[] args){SpringApplication.run(AuthServiceApplication.class,args);}

    @Bean PasswordEncoder passwordEncoder(){return new BCryptPasswordEncoder();}

    @Bean CommandLineRunner seedUsers(UserRepository repo, PasswordEncoder encoder){
        return args -> {
            if(repo.count()>0) return;
            add(repo,encoder,"flood.recorder","Password123!","FLOOD_RECORDER","FLOOD","Ward 1");
            add(repo,encoder,"flood.supervisor","Password123!","FLOOD_SUPERVISOR","FLOOD","");
            add(repo,encoder,"drought.recorder","Password123!","DROUGHT_RECORDER","DROUGHT","Ward 2");
            add(repo,encoder,"drought.supervisor","Password123!","DROUGHT_SUPERVISOR","DROUGHT","");
            add(repo,encoder,"fire.recorder","Password123!","FIRE_RECORDER","FIRE","Ward 3");
            add(repo,encoder,"fire.supervisor","Password123!","FIRE_SUPERVISOR","FIRE","");
            add(repo,encoder,"zoonotic.recorder","Password123!","ZOONOTIC_RECORDER","ZOONOTIC","Ward 4");
            add(repo,encoder,"zoonotic.supervisor","Password123!","ZOONOTIC_SUPERVISOR","ZOONOTIC","");
            add(repo,encoder,"mining.recorder","Password123!","MINING_RECORDER","MINING","Ward 5");
            add(repo,encoder,"mining.supervisor","Password123!","MINING_SUPERVISOR","MINING","");
            add(repo,encoder,"national.user","Password123!","NATIONAL_USER","ALL","");
            add(repo,encoder,"provincial.admin","Password123!","PROVINCIAL_ADMIN","ALL","");
        };
    }
    private void add(UserRepository repo,PasswordEncoder encoder,String username,String password,String role,String hazard,String ward){
        UserAccount u=new UserAccount();u.setUsername(username);u.setPasswordHash(encoder.encode(password));u.setRole(role);u.setHazard(hazard);u.setWard(ward);u.setDistrict("Rushinga");u.setProvince("Mashonaland Central");repo.save(u);
    }
}

@Entity @Table(name="users",uniqueConstraints=@UniqueConstraint(columnNames="username"))
class UserAccount {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    private String username,passwordHash,role,hazard,ward,district,province;
    private boolean enabled=true;
    protected UserAccount(){}
    public Long getId(){return id;} public String getUsername(){return username;} public void setUsername(String v){username=v;}
    public String getPasswordHash(){return passwordHash;} public void setPasswordHash(String v){passwordHash=v;}
    public String getRole(){return role;} public void setRole(String v){role=v;} public String getHazard(){return hazard;} public void setHazard(String v){hazard=v;}
    public String getWard(){return ward;} public void setWard(String v){ward=v;} public String getDistrict(){return district;} public void setDistrict(String v){district=v;}
    public String getProvince(){return province;} public void setProvince(String v){province=v;} public boolean isEnabled(){return enabled;}
}
@Repository interface UserRepository extends org.springframework.data.jpa.repository.JpaRepository<UserAccount,Long>{Optional<UserAccount>findByUsername(String username);}
@Service class JwtService {
    private final SecretKey key;
    JwtService(@Value("${dpdms.jwt-secret}")String secret){key=Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));}
    String generate(UserAccount u){Instant now=Instant.now();return Jwts.builder().subject(u.getUsername()).claim("role",u.getRole()).claim("hazard",u.getHazard()).claim("ward",u.getWard()).claim("district",u.getDistrict()).claim("province",u.getProvince()).issuedAt(Date.from(now)).expiration(Date.from(now.plus(Duration.ofHours(2)))).signWith(key).compact();}
    Claims validate(String token){return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();}
}
class LoginRequest{public String username;public String password;}
class RegisterRequest{public String username;public String password;public String hazard;public String ward;}
class AuthResponse{public String token,username,role,hazard,ward,district,province;AuthResponse(String token,UserAccount u){this.token=token;this.username=u.getUsername();this.role=u.getRole();this.hazard=u.getHazard();this.ward=u.getWard();this.district=u.getDistrict();this.province=u.getProvince();}}
@RestController @RequestMapping("/api/v1/auth") class AuthController {
    private final UserRepository repo;private final PasswordEncoder encoder;private final JwtService jwt;
    AuthController(UserRepository repo,PasswordEncoder encoder,JwtService jwt){this.repo=repo;this.encoder=encoder;this.jwt=jwt;}
    @PostMapping("/login") ResponseEntity<?> login(@RequestBody LoginRequest q){UserAccount u=repo.findByUsername(q.username).orElseThrow(()->new IllegalArgumentException("Invalid username or password."));if(!u.isEnabled()||!encoder.matches(q.password,u.getPasswordHash()))throw new IllegalArgumentException("Invalid username or password.");return ResponseEntity.ok(new AuthResponse(jwt.generate(u),u));}
    @PostMapping("/register") ResponseEntity<?> register(@RequestBody RegisterRequest q){
        if(q.username==null||q.password==null||q.hazard==null||q.ward==null)return ResponseEntity.badRequest().body(Map.of("error","VALIDATION_ERROR"));
        String h=q.hazard.trim().toUpperCase(Locale.ROOT);if(!Set.of("FLOOD","DROUGHT","FIRE","ZOONOTIC","MINING").contains(h))return ResponseEntity.badRequest().body(Map.of("error","INVALID_HAZARD"));
        if(repo.findByUsername(q.username).isPresent())return ResponseEntity.status(409).body(Map.of("error","USERNAME_EXISTS"));
        UserAccount u=new UserAccount();u.setUsername(q.username.trim());u.setPasswordHash(encoder.encode(q.password));u.setRole(h+"_RECORDER");u.setHazard(h);u.setWard(q.ward.trim());u.setDistrict("Rushinga");u.setProvince("Mashonaland Central");repo.save(u);return ResponseEntity.status(201).body(new AuthResponse(jwt.generate(u),u));
    }
    @GetMapping("/validate") ResponseEntity<?> validate(@RequestParam String token){Claims c=jwt.validate(token);return ResponseEntity.ok(Map.of("username",c.getSubject(),"role",c.get("role",String.class),"hazard",c.get("hazard",String.class),"ward",Objects.toString(c.get("ward"),""),"district",Objects.toString(c.get("district"),"Rushinga"),"province",Objects.toString(c.get("province"),"Mashonaland Central")));}
}
@RestControllerAdvice class AuthErrors {@ExceptionHandler(IllegalArgumentException.class)ResponseEntity<?> unauthorized(IllegalArgumentException e){return ResponseEntity.status(401).body(Map.of("error","UNAUTHORIZED","message",e.getMessage()));}}
