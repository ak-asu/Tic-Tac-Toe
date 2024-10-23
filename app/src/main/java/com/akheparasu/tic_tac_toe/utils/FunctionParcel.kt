package com.akheparasu.tic_tac_toe.utils

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
class FunctionParcel(val function: () -> Unit) : Parcelable
