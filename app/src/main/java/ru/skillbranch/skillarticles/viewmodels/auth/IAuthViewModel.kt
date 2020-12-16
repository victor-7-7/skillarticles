package ru.skillbranch.skillarticles.viewmodels.auth

interface IAuthViewModel {
    /**
     * обработка авторизации пользователя
     */
    fun handleLogin(login: String, pass: String, dest: Int?)

    /**
     * обработка регистрации пользователя
     */
    fun handleRegister(name: String, email: String, pass: String, dest: Int?)
}