package se.gigurra.franklin.mongoimpl

/**
  * Created by kjolh on 12/30/2015.
  */
object Escape {

  private def needsEscaping(s: String): Boolean = {
    s.length > 0 && {
      val c = s.charAt(0)
      c == '$' || c == '.' || c == EscapeChar.value
    }
  }

  def apply(raw: String): String = {
    if (needsEscaping(raw)) {
      EscapeChar.value + raw
    } else {
      raw
    }
  }
}

object UnEscape {

  private def isEscaped(s: String): Boolean = {
    s.length > 0 && s.charAt(0) == EscapeChar.value
  }

  def apply(stored: String): String = {
    if (isEscaped(stored)) {
      stored.drop(1)
    } else {
      stored
    }
  }
}

object EscapeChar {
  val value = '\\'
}