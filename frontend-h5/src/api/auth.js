import request from './request'

export const register = (data) => request.post('/api/auth/register', data)
export const login = (data) => request.post('/api/auth/login', data)
