# auth API

Auth API v2

认证和用户管理相关的API

**File**: `v2/auth.ts`

## Types

### User

Auth and User Types

```typescript
interface User {
  userId: string;
    username: string;
    email: string;
    role: string;
    status: number;
    avatar?: string;
    createTime: string;
    lastLoginTime?: string;
}
```

### LoginParams

Auth and User Types

```typescript
interface LoginParams {
  username: string;
    password: string;
}
```

### LoginResult

Auth and User Types

```typescript
interface LoginResult {
  token: string;
    user: User;
}
```

### RegisterParams

Auth and User Types

```typescript
interface RegisterParams {
  username: string;
    password: string;
    email: string;
    role?: string;
}
```

## Functions

### login

Authentication functions

```typescript
const login = (data: LoginParams) => { ... }
```

### register

Authentication functions

```typescript
const register = (data: RegisterParams) => { ... }
```

### logout

Authentication functions

```typescript
const logout = () => { ... }
```

### getInfo

Authentication functions

```typescript
const getInfo = () => { ... }
```

### refreshToken

Authentication functions

```typescript
const refreshToken = () => { ... }
```

### getUserList

User management functions

```typescript
const getUserList = (params?: { page?: number; size?: number; query?: string }) => { ... }
```

### getUserDetail

User management functions

```typescript
const getUserDetail = (userId: string) => { ... }
```

### createUser

User management functions

```typescript
const createUser = (data: Omit<User, 'userId' | 'createTime' | 'lastLoginTime'>) => { ... }
```

### updateUser

User management functions

```typescript
const updateUser = (userId: string, data: Partial<User>) => { ... }
```

### deleteUser

User management functions

```typescript
const deleteUser = (userId: string) => { ... }
```

### changePassword

User management functions

```typescript
const changePassword = (userId: string, oldPassword: string, newPassword: string) => { ... }
```

### resetPassword

User management functions

```typescript
const resetPassword = (userId: string) => { ... }
```

## API Endpoints

- `/api/v2`

