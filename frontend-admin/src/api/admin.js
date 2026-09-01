import request from './request'

// ===== 登录 =====
export const adminLogin = (data) => request.post('/api/auth/login', data)

// ===== 用户管理 =====
export const getUserList = (params) => request.get('/api/admin/user/list', { params })
export const getUserDetail = (id) => request.get(`/api/admin/user/detail/${id}`)
export const getUserPosts = (id, params) => request.get(`/api/admin/user/posts/${id}`, { params })
export const getUserApplies = (id, params) => request.get(`/api/admin/user/applies/${id}`, { params })
export const getUserReviews = (id, params) => request.get(`/api/admin/user/reviews/${id}`, { params })
export const banUser = (id) => request.post(`/api/admin/user/ban/${id}`)
export const unbanUser = (id) => request.post(`/api/admin/user/unban/${id}`)
export const resetCredit = (data) => request.put('/api/admin/user/credit', data)

// ===== 帖子管理 =====
export const getPostList = (params) => request.get('/api/admin/post/list', { params })
export const takedownPost = (id) => request.post(`/api/admin/post/takedown/${id}`)
export const auditPost = (id, data) => request.post(`/api/admin/post/audit/${id}`, data)
export const deletePost = (id) => request.delete(`/api/admin/post/delete/${id}`)

// ===== 标签管理 =====
export const getTagList = () => request.get('/api/admin/tag/list')
export const getTagCategories = () => request.get('/api/admin/tag/categories')
export const addTag = (data) => request.post('/api/admin/tag/add', data)
export const updateTag = (data) => request.put('/api/admin/tag/update', data)
export const disableTag = (id) => request.post(`/api/admin/tag/disable/${id}`)

// ===== 数据统计 =====
export const getStatsOverview = () => request.get('/api/admin/stats/overview')
export const getPostTrend = (params) => request.get('/api/admin/stats/posts', { params })
export const getSuccessTrend = (params) => request.get('/api/admin/stats/success', { params })
export const getActiveTrend = (params) => request.get('/api/admin/stats/active', { params })
export const getCategoryTop5 = () => request.get('/api/admin/stats/category')
export const getCreditDistribution = () => request.get('/api/admin/stats/credit')
export const getUserGrowthTrend = (params) => request.get('/api/admin/stats/user-growth', { params })
export const getRetention = (params) => request.get('/api/admin/stats/retention', { params })
export const getFunnel = (params) => request.get('/api/admin/stats/funnel', { params })

// ===== 举报处理（V1.1）=====
export const getReportList = (params) => request.get('/api/admin/report/list', { params })
export const getReportDetail = (id) => request.get(`/api/admin/report/detail/${id}`)
export const handleReport = (id, data) => request.post(`/api/admin/report/handle/${id}`, data)

// ===== 系统公告 =====
export const getAnnouncementList = (params) => request.get('/api/admin/announcement/list', { params })
export const addAnnouncement = (data) => request.post('/api/admin/announcement/add', data)
export const updateAnnouncement = (data) => request.put('/api/admin/announcement/update', data)
export const publishAnnouncement = (id) => request.post(`/api/admin/announcement/publish/${id}`)
export const closeAnnouncement = (id) => request.post(`/api/admin/announcement/close/${id}`)
export const deleteAnnouncement = (id) => request.delete(`/api/admin/announcement/${id}`)

// ===== 数据导出 =====
export const exportUsers = () => request.get('/api/admin/stats/export/users', { responseType: 'blob' })
export const exportPosts = () => request.get('/api/admin/stats/export/posts', { responseType: 'blob' })
export const exportStats = (params) => request.get('/api/admin/stats/export/stats', { params, responseType: 'blob' })
