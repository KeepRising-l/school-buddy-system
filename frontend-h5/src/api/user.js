import request from './request'

export const getProfile = () => request.get('/api/user/profile')
export const updateProfile = (data) => request.put('/api/user/profile', data)
export const getMyTags = () => request.get('/api/user/tags')
export const setMyTags = (data) => request.put('/api/user/tags', data)
export const getMyPosts = (params) => request.get('/api/user/posts', { params })
export const getMyApplies = (params) => request.get('/api/user/applies', { params })
export const uploadAvatar = (formData) => request.post('/api/file/upload', formData, {
  headers: { 'Content-Type': 'multipart/form-data' }
})
