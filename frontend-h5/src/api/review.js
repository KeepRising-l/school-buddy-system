import request from './request'

export const submitReview = (data) => request.post('/api/review/submit', data)
export const getPostReviews = (postId) => request.get(`/api/review/list/${postId}`)
export const getUserReviews = (userId) => request.get(`/api/review/user/${userId}`)
