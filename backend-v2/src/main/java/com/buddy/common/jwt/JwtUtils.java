package com.buddy.common.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 工具类
 * 负责令牌的生成、解析与校验
 * 迁移说明：jjwt 0.11 -> 0.12，API 变化（parserBuilder -> Jwts.parser().verifyWith，setSubject -> subject 链式）
 */
@Slf4j
@Component
public class JwtUtils {

    /** HS256 要求密钥不少于 256 bit（32 字节） */
    private static final int MIN_SECRET_BYTES = 32;

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expiration;

    private SecretKey key;

    @jakarta.annotation.PostConstruct
    public void init() {
        // 密钥缺失或不达标时直接启动失败，避免静默使用可预测的密钥签发 Token
        if (secret == null || secret.trim().isEmpty()) {
            throw new IllegalStateException(
                    "JWT 密钥未配置：请通过环境变量 JWT_SECRET 或本地 application-local.yml 设置（至少 32 字符）");
        }
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "JWT 密钥长度不足：当前 " + keyBytes.length + " 字节，HS256 至少需要 " + MIN_SECRET_BYTES + " 字节");
        }
        this.key = Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * 生成 Token
     *
     * @param userId    用户ID
     * @param studentNo 学号
     * @return Token 字符串
     */
    public String generateToken(Long userId, String studentNo) {
        Date now = new Date();
        Date expireDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("studentNo", studentNo)
                .issuedAt(now)
                .expiration(expireDate)
                .signWith(key)
                .compact();
    }

    /**
     * 从 Token 中获取用户ID
     */
    public Long getUserIdFromToken(String token) {
        Claims claims = parseToken(token);
        return Long.parseLong(claims.getSubject());
    }

    /**
     * 从 Token 中获取学号
     */
    public String getStudentNoFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.get("studentNo", String.class);
    }

    /**
     * 校验 Token 是否有效
     */
    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (Exception e) {
            log.warn("Token 校验失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 解析 Token，返回 Claims
     */
    private Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
