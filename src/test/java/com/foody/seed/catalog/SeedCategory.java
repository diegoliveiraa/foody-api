package com.foody.seed.catalog;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public enum SeedCategory {

	LANCHES("Lanches", 22, 45,
			List.of(SeedProduct.prototype("Smash duplo", "34.90",
					"Dois blends de 90 g, cheddar e cebola caramelizada no pão brioche."),
					SeedProduct.prototype("Frango crispy", "29.90",
							"Sobrecoxa empanada, maionese de ervas e alface americana."),
					SeedProduct.prototype("Veggie de grão-de-bico", "27.90",
							"Hambúrguer de grão-de-bico, picles da casa e molho de iogurte."),
					SeedProduct.of("X-salada", "Blend de 150 g, queijo prato, alface, tomate e maionese da casa."),
					SeedProduct.of("X-bacon", "Blend de 150 g, bacon crocante, queijo prato e molho barbecue."),
					SeedProduct.of("Cheddar melt",
							"Blend de 180 g, cheddar cremoso e cebola na manteiga no pão australiano."),
					SeedProduct.of("Sanduíche de pernil", "Pernil desfiado, vinagrete e queijo no pão francês."),
					SeedProduct.of("Bauru", "Rosbife, queijo derretido, tomate e picles no pão francês."))),

	PIZZAS("Pizzas", 45, 89, List.of(
			SeedProduct.prototype("Margherita", "49.90", "Molho de tomate, muçarela e manjericão fresco. 8 fatias."),
			SeedProduct.prototype("Calabresa", "52.90", "Calabresa fatiada, cebola roxa e azeitonas. 8 fatias."),
			SeedProduct.of("Portuguesa", "Presunto, ovos, cebola, ervilha e muçarela. 8 fatias."),
			SeedProduct.of("Quatro queijos", "Muçarela, provolone, parmesão e gorgonzola. 8 fatias."),
			SeedProduct.of("Frango com catupiry", "Frango desfiado temperado e catupiry original. 8 fatias."),
			SeedProduct.of("Pepperoni", "Pepperoni fatiado e muçarela. 8 fatias."),
			SeedProduct.of("Napolitana", "Muçarela, tomate em rodelas, parmesão e orégano. 8 fatias."),
			SeedProduct.of("Rúcula com tomate seco", "Muçarela de búfala, rúcula e tomate seco. 8 fatias."))),

	PRATOS("Pratos", 32, 69, List.of(
			SeedProduct.of("Parmegiana de frango", "Filé empanado, molho de tomate e muçarela, com arroz e fritas."),
			SeedProduct.of("Picanha na chapa", "Picanha fatiada com arroz, farofa, vinagrete e fritas."),
			SeedProduct.of("Strogonoff de carne", "Tiras de alcatra ao molho cremoso, arroz branco e batata palha."),
			SeedProduct.of("Feijoada", "Feijão preto com carnes, arroz, couve refogada, farofa e laranja."),
			SeedProduct.of("Frango grelhado com legumes",
					"Peito de frango grelhado, legumes salteados e arroz integral."),
			SeedProduct.of("Moqueca de peixe", "Peixe branco no leite de coco e dendê, com arroz e pirão."),
			SeedProduct.of("Escondidinho de carne seca", "Purê de mandioca gratinado com carne seca desfiada."))),

	SALADAS("Saladas", 24, 42,
			List.of(SeedProduct.of("Caesar", "Alface romana, frango grelhado, croutons, parmesão e molho caesar."),
					SeedProduct.of("Caprese", "Tomate, muçarela de búfala, manjericão e azeite."),
					SeedProduct.of("Salada tropical", "Folhas verdes, manga, palmito, castanhas e molho de maracujá."),
					SeedProduct.of("Salada de quinoa", "Quinoa, grão-de-bico, pepino, tomate e hortelã."),
					SeedProduct.of("Salada grega", "Pepino, tomate, cebola roxa, azeitonas e queijo feta."),
					SeedProduct.of("Salada de salmão", "Mix de folhas, salmão grelhado, abacate e gergelim."))),

	PORCOES("Porções", 28, 65,
			List.of(SeedProduct.of("Batata frita", "Batata palito crocante com sal e alecrim. Serve duas pessoas."),
					SeedProduct.of("Mandioca frita", "Mandioca cozida e frita, com maionese de alho."),
					SeedProduct.of("Isca de frango", "Tiras de frango empanadas com molho de mostarda e mel."),
					SeedProduct.of("Anéis de cebola", "Anéis de cebola empanados com molho barbecue."),
					SeedProduct.of("Calabresa acebolada", "Calabresa fatiada na chapa com cebola e pão."),
					SeedProduct.of("Bolinho de bacalhau", "Doze bolinhos de bacalhau com limão."),
					SeedProduct.of("Pastéis sortidos", "Oito minipastéis de carne, queijo e palmito."))),

	BEBIDAS("Bebidas", 6, 16,
			List.of(SeedProduct.prototype("Limonada da casa", "12.00", "Limão espremido na hora, 500 ml."),
					SeedProduct.prototype("Refrigerante lata", "7.50", "350 ml. Consulte os sabores."),
					SeedProduct.of("Suco de laranja", "Laranja espremida na hora, 500 ml."),
					SeedProduct.of("Água mineral", "Sem gás, 500 ml."),
					SeedProduct.of("Água com gás", "Com gás, 500 ml."),
					SeedProduct.of("Chá gelado", "Chá preto com limão e hortelã, 400 ml."),
					SeedProduct.of("Suco de maracujá", "Polpa de maracujá batida com água, 500 ml."))),

	SOBREMESAS("Sobremesas", 12, 28,
			List.of(SeedProduct.prototype("Brownie com sorvete", "18.90",
					"Brownie de chocolate meio amargo e uma bola de creme."),
					SeedProduct.of("Pudim de leite", "Pudim de leite condensado com calda de caramelo."),
					SeedProduct.of("Petit gâteau", "Bolinho de chocolate com recheio cremoso e sorvete de creme."),
					SeedProduct.of("Mousse de maracujá", "Mousse aerada com calda da fruta."),
					SeedProduct.of("Torta de limão", "Massa amanteigada, creme de limão e merengue maçaricado."),
					SeedProduct.of("Açaí na tigela", "Açaí batido com banana, granola e mel, 400 ml."))),

	COMBOS("Combos", 39, 79,
			List.of(SeedProduct.of("Combo smash", "Smash duplo, batata frita e refrigerante lata."),
					SeedProduct.of("Combo frango crispy", "Frango crispy, batata frita e limonada da casa."),
					SeedProduct.of("Combo casal", "Dois lanches da casa, uma porção de batata e dois refrigerantes."),
					SeedProduct.of("Combo família", "Pizza grande, porção de batata e refrigerante de 2 litros."),
					SeedProduct.of("Combo kids", "Mini-hambúrguer, batata frita, suco e uma surpresa."),
					SeedProduct.of("Combo executivo", "Prato do dia, refrigerante lata e sobremesa.")));

	private final String label;

	private final int minPrice;

	private final int maxPrice;

	private final List<SeedProduct> products;

	SeedCategory(String label, int minPrice, int maxPrice, List<SeedProduct> products) {
		this.label = label;
		this.minPrice = minPrice;
		this.maxPrice = maxPrice;
		this.products = products;
	}

	public static Set<String> prototypeNames() {
		return Arrays.stream(values())
			.flatMap(category -> category.products().stream())
			.filter(SeedProduct::isPrototype)
			.map(SeedProduct::name)
			.collect(Collectors.toUnmodifiableSet());
	}

	public String label() {
		return label;
	}

	public int minPrice() {
		return minPrice;
	}

	public int maxPrice() {
		return maxPrice;
	}

	public List<SeedProduct> products() {
		return products;
	}

}
