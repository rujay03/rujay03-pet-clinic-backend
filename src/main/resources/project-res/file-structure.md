src/main/java/com/petclinic/backend
├── common
│   ├── exception
│   ├── dto
│   └── util
├── config
│   ├── SecurityConfig.java
│   ├── CorsConfig.java
│   └── SessionConfig.java
├── auth
│   ├── web
│   │   └── AuthController.java
│   ├── service
│   │   └── AuthService.java
│   ├── dto
│   │   ├── SignupRequest.java
│   │   ├── LoginRequest.java
│   │   └── MeResponse.java
│   └── mapper (optional)
├── user
│   ├── entity
│   │   ├── User.java
│   │   ├── Role.java
│   │   └── UserRole.java
│   ├── repo
│   │   ├── UserRepository.java
│   │   └── RoleRepository.java
│   └── service
│       └── UserDetailsServiceImpl.java
├── owner
│   ├── entity
│   │   └── PetOwner.java
│   ├── repo
│   │   └── PetOwnerRepository.java
│   └── service
│       └── PetOwnerService.java
└── PetClinicApplication.java
