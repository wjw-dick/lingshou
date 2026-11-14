const config = require('./config')

function toast(title) {
  wx.showToast({
    title: title || '请求失败',
    icon: 'none',
    duration: 2200
  })
}

function buildQuery(params) {
  if (!params) {
    return ''
  }
  const parts = []
  Object.keys(params).forEach(function (key) {
    const value = params[key]
    if (value !== undefined && value !== null && value !== '') {
      parts.push(encodeURIComponent(key) + '=' + encodeURIComponent(value))
    }
  })
  return parts.join('&')
}

function redirectLogin() {
  const pages = getCurrentPages()
  const current = pages.length ? pages[pages.length - 1].route : ''
  if (current === 'pages/login/index') {
    return
  }
  wx.reLaunch({ url: '/pages/login/index' })
}

function request(options) {
  const settings = options || {}
  const method = (settings.method || 'GET').toUpperCase()
  const token = wx.getStorageSync('token')
  const header = {
    'Content-Type': 'application/json'
  }
  if (settings.auth !== false && token) {
    header.Authorization = 'Bearer ' + token
  }
  let url = config.baseUrl + settings.url
  const query = buildQuery(settings.params)
  if (query) {
    url += (url.indexOf('?') >= 0 ? '&' : '?') + query
  }
  if (settings.loading) {
    wx.showLoading({ title: settings.loadingText || '加载中', mask: true })
  }
  return new Promise(function (resolve, reject) {
    wx.request({
      url: url,
      method: method,
      data: settings.data,
      header: header,
      timeout: 15000,
      success: function (res) {
        if (settings.loading) {
          wx.hideLoading()
        }
        const body = res.data
        if (res.statusCode === 401) {
          wx.removeStorageSync('token')
          wx.removeStorageSync('user')
          toast('登录已过期，请重新登录')
          redirectLogin()
          reject(new Error('unauthorized'))
          return
        }
        if (res.statusCode >= 200 && res.statusCode < 300) {
          if (body && typeof body === 'object' && Object.prototype.hasOwnProperty.call(body, 'code')) {
            if (body.code === 0) {
              resolve(body.data)
            } else {
              const message = body.message || '请求处理失败'
              toast(message)
              reject(new Error(message))
            }
            return
          }
          resolve(body)
          return
        }
        const message = (body && (body.message || body.error)) || ('网络异常（' + res.statusCode + '）')
        toast(message)
        reject(new Error(message))
      },
      fail: function (err) {
        if (settings.loading) {
          wx.hideLoading()
        }
        toast('网络连接失败，请检查后端服务')
        reject(err)
      }
    })
  })
}

function get(url, params, options) {
  return request(Object.assign({ url: url, method: 'GET', params: params }, options || {}))
}

function post(url, data, params, options) {
  return request(Object.assign({ url: url, method: 'POST', data: data, params: params }, options || {}))
}

function put(url, data, params, options) {
  return request(Object.assign({ url: url, method: 'PUT', data: data, params: params }, options || {}))
}

function del(url, params, options) {
  return request(Object.assign({ url: url, method: 'DELETE', params: params }, options || {}))
}

module.exports = {
  request: request,
  get: get,
  post: post,
  put: put,
  del: del,
  toast: toast
}
