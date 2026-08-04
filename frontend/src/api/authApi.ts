import client from "./client";
import { AuthUser, LoginRequest, PlatformUser, RegisterRequest } from "../types";

export const authApi = {
  register: (data: RegisterRequest) => client.post<AuthUser>("/api/auth/register", data).then((r) => r.data),
  login: (data: LoginRequest) => client.post<AuthUser>("/api/auth/login", data).then((r) => r.data),
  listUsers: (role?: string) =>
    client.get<PlatformUser[]>("/api/auth/users", { params: role ? { role } : {} }).then((r) => r.data),
  deleteUser: (userId: number) => client.delete(`/api/auth/users/${userId}`)
};
