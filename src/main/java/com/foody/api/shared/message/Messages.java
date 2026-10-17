package com.foody.api.shared.message;

public enum Messages {

	VALIDATION_FAILED("Os dados enviados são inválidos"), MALFORMED_REQUEST("O corpo da requisição está malformado"),
	INVALID_PARAMETER("Parâmetro inválido"), INVALID_SORT("Ordenação não permitida"),
	INVALID_PAGE_SIZE("Quantidade de itens por página não permitida. Valores aceitos"),
	RESOURCE_NOT_FOUND("Recurso não encontrado"), METHOD_NOT_ALLOWED("Método não permitido para este recurso"),
	UNSUPPORTED_MEDIA_TYPE("Tipo de conteúdo não suportado"),
	CONCURRENT_UPDATE("O registro foi alterado por outra operação. Recarregue e tente novamente"),
	DATA_CONFLICT("A operação conflita com dados existentes"),
	REQUEST_FAILED("Não foi possível processar a requisição"),
	INTERNAL_ERROR("Erro inesperado. Tente novamente em instantes"), UNAUTHENTICATED("Autenticação necessária"),
	FORBIDDEN("Você não tem permissão para acessar este recurso"),
	RATE_LIMITED("Muitas tentativas, tente novamente em instantes"), INVALID_CREDENTIALS("E-mail ou senha inválidos"),
	EMAIL_ALREADY_REGISTERED("Este e-mail já está cadastrado"),
	REFRESH_TOKEN_INVALID("Sua sessão expirou, entre novamente"),
	PASSWORD_TOO_SHORT("A senha é curta demais. Mínimo de caracteres"),
	PASSWORD_TOO_LONG("A senha é longa demais. Máximo de bytes"),
	CANNOT_CHANGE_OWN_ACCOUNT("Você não pode alterar a sua própria conta"),
	LAST_ACTIVE_ADMIN("É preciso manter ao menos um administrador ativo");

	private final String text;

	Messages(String text) {
		this.text = text;
	}

	public String text() {
		return text;
	}

}
