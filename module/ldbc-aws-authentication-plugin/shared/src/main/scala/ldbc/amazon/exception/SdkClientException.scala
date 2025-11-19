package ldbc.amazon.exception

class SdkClientException(message: String) extends RuntimeException:

  final override def getMessage: String = message
