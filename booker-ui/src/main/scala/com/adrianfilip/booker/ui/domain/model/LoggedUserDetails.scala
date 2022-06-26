package com.adrianfilip.booker.ui.domain.model

import com.adrianfilip.booker.domain.model.User

case class LoggedUserDetails(user: User, token: String)
