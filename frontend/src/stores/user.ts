import { ref } from 'vue'
import { defineStore } from 'pinia'

export const useUserStore = defineStore('user', () => {
  const username = ref('')
  const userId = ref<number|null>(null)
  const isLoggedIn = ref(false)

  function login(name: string, id: number) {
    username.value = name
    userId.value = id
    isLoggedIn.value = true
  }
  function logout() {
    username.value = ''
    userId.value = null
    isLoggedIn.value = false
  }

  return { username, userId, isLoggedIn, login, logout }
}) 