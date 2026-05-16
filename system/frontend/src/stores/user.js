import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useUserStore = defineStore('user', () => {
  const userId = ref(null)
  const username = ref('')

  function setUser(user) {
    userId.value = user.userId
    username.value = user.username
  }

  function clearUser() {
    userId.value = null
    username.value = ''
  }

  function isLoggedIn() {
    return !!localStorage.getItem('token')
  }

  return { userId, username, setUser, clearUser, isLoggedIn }
})
