package com.example.bankcards.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "JWT токен")
public record JwtResponse(

        @Schema(description = "JWT токен", example = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJBRE1JTiIsImlhdCI6MTc2MzMxMDI0NCwiZXhwIjoxNzYzMzEzODQ0fQ.BQjJzGBzZ6DBL1JpSi_zPMypnl3ARu3Kp2mbX2r5omw") 
        String token) {

}
