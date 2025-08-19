package com.example.rsrtest.data

// Base de datos completa de productos PepsiCo
object ProductData {
    fun getAllPepsiCoProducts(): List<Product> = listOf(
        // ===== BEBIDAS PEPSI =====
        Product(
            id = "pepsi-cola-600ml",
            name = "Pepsi Cola 600ml",
            brand = ProductBrand.PEPSI.displayName,
            category = ProductCategory.BEBIDAS.displayName,
            subcategory = "Refrescos de Cola",
            description = "La auténtica bebida de cola con el sabor único de Pepsi",
            price = 18.50,
            barcode = "7501234567890",
            weight = "600ml",
            keywords = "pepsi, cola, refresco, bebida, 600ml",
            detectionKeywords = "pepsi,cola,bottle,soda,beverage,drink,can"
        ),
        Product(
            id = "pepsi-cola-355ml",
            name = "Pepsi Cola Lata 355ml",
            brand = ProductBrand.PEPSI.displayName,
            category = ProductCategory.BEBIDAS.displayName,
            subcategory = "Refrescos de Cola",
            description = "Pepsi Cola en lata, perfecto para llevar",
            price = 15.00,
            barcode = "7501234567891",
            weight = "355ml",
            keywords = "pepsi, cola, lata, refresco, bebida, 355ml",
            detectionKeywords = "pepsi,pepsi black,cola,can,soda,beverage,drink"
        ),
        Product(
            id = "pepsi-cola-3l",
            name = "Pepsi Cola 3L",
            brand = ProductBrand.PEPSI.displayName,
            category = ProductCategory.BEBIDAS.displayName,
            subcategory = "Refrescos de Cola",
            description = "Pepsi Cola familiar de 3 litros",
            price = 45.00,
            barcode = "7501234567892",
            weight = "3L",
            keywords = "pepsi, cola, refresco, familiar, 3 litros",
            detectionKeywords = "pepsi,cola,bottle,soda,beverage,drink,big"
        ),

        // ===== 7UP =====
        Product(
            id = "7up-600ml",
            name = "7UP 600ml",
            brand = ProductBrand.SEVEN_UP.displayName,
            category = ProductCategory.BEBIDAS.displayName,
            subcategory = "Refrescos Lima-Limón",
            description = "Refresco de lima-limón sin cafeína con sabor refrescante",
            price = 18.50,
            barcode = "7501234567893",
            weight = "600ml",
            keywords = "7up, lima, limon, refresco, sin cafeína, 600ml",
            detectionKeywords = "7up,seven up,lime,lemon,soda,beverage,drink,bottle"
        ),
        Product(
            id = "7up-355ml",
            name = "7UP Lata 355ml",
            brand = ProductBrand.SEVEN_UP.displayName,
            category = ProductCategory.BEBIDAS.displayName,
            subcategory = "Refrescos Lima-Limón",
            description = "7UP en lata de 355ml",
            price = 15.00,
            barcode = "7501234567894",
            weight = "355ml",
            keywords = "7up, lima, limon, lata, refresco, 355ml",
            detectionKeywords = "7up,seven up,lime,lemon,soda,beverage,drink,can"
        ),

        // ===== MIRINDA =====
        Product(
            id = "mirinda-naranja-600ml",
            name = "Mirinda Naranja 600ml",
            brand = ProductBrand.MIRINDA.displayName,
            category = ProductCategory.BEBIDAS.displayName,
            subcategory = "Refrescos de Naranja",
            description = "Refresco sabor naranja con burbujas naturales",
            price = 18.50,
            barcode = "7501234567895",
            weight = "600ml",
            keywords = "mirinda, naranja, refresco, burbujas, 600ml",
            detectionKeywords = "mirinda,orange,naranja,soda,beverage,drink,bottle"
        ),

        // ===== MOUNTAIN DEW =====
        Product(
            id = "mountain-dew-600ml",
            name = "Mountain Dew 600ml",
            brand = ProductBrand.MOUNTAIN_DEW.displayName,
            category = ProductCategory.BEBIDAS.displayName,
            subcategory = "Refrescos Energizantes",
            description = "Refresco con cafeína y sabor cítrico único",
            price = 20.00,
            barcode = "7501234567896",
            weight = "600ml",
            keywords = "mountain dew, cafeína, citrico, energizante, 600ml",
            detectionKeywords = "mountain dew,citrus,energy,soda,beverage,drink,bottle"
        ),

        // ===== GATORADE =====
        Product(
            id = "gatorade-naranja-500ml",
            name = "Gatorade Naranja 500ml",
            brand = ProductBrand.GATORADE.displayName,
            category = ProductCategory.BEBIDAS.displayName,
            subcategory = "Bebidas Deportivas",
            description = "Bebida hidratante sabor naranja para deportistas",
            price = 25.00,
            barcode = "7501234567897",
            weight = "500ml",
            keywords = "gatorade, naranja, deportiva, hidratante, electrolitos, 500ml",
            detectionKeywords = "gatorade,sports drink,orange,hydration,beverage,bottle"
        ),
        Product(
            id = "gatorade-uva-500ml",
            name = "Gatorade Uva 500ml",
            brand = ProductBrand.GATORADE.displayName,
            category = ProductCategory.BEBIDAS.displayName,
            subcategory = "Bebidas Deportivas",
            description = "Bebida hidratante sabor uva para deportistas",
            price = 25.00,
            barcode = "7501234567898",
            weight = "500ml",
            keywords = "gatorade, uva, deportiva, hidratante, electrolitos, 500ml",
            detectionKeywords = "gatorade,sports drink,grape,uva,hydration,beverage,bottle"
        ),

        // ===== TROPICANA =====
        Product(
            id = "tropicana-naranja-1l",
            name = "Tropicana Naranja 1L",
            brand = ProductBrand.TROPICANA.displayName,
            category = ProductCategory.JUGOS.displayName,
            subcategory = "Jugos de Naranja",
            description = "Jugo de naranja 100% natural sin conservadores",
            price = 35.00,
            barcode = "7501234567899",
            weight = "1L",
            keywords = "tropicana, naranja, jugo, natural, sin conservadores, 1L",
            detectionKeywords = "tropicana,orange juice,natural,juice,beverage,bottle"
        ),

        // ===== AQUAFINA =====
        Product(
            id = "aquafina-600ml",
            name = "Aquafina 600ml",
            brand = ProductBrand.AQUAFINA.displayName,
            category = ProductCategory.AGUA.displayName,
            subcategory = "Agua Purificada",
            description = "Agua purificada con proceso de ósmosis inversa",
            price = 12.00,
            barcode = "7501234567900",
            weight = "600ml",
            keywords = "aquafina, agua, purificada, osmosis, 600ml",
            detectionKeywords = "aquafina,water,purified,bottle,beverage"
        ),
        Product(
            id = "aquafina-1.5l",
            name = "Aquafina 1.5L",
            brand = ProductBrand.AQUAFINA.displayName,
            category = ProductCategory.AGUA.displayName,
            subcategory = "Agua Purificada",
            description = "Agua purificada familiar de 1.5 litros",
            price = 18.00,
            barcode = "7501234567901",
            weight = "1.5L",
            keywords = "aquafina, agua, purificada, familiar, 1.5L",
            detectionKeywords = "aquafina,water,purified,bottle,beverage,big"
        ),

        // ===== SABRITAS / LAY'S =====
        Product(
            id = "sabritas-clasicas-45g",
            name = "Sabritas Clásicas 45g",
            brand = ProductBrand.SABRITAS.displayName,
            category = ProductCategory.SNACKS.displayName,
            subcategory = "Papas Fritas",
            description = "Papas fritas con el sabor clásico que todos aman",
            price = 18.00,
            barcode = "7501234567902",
            weight = "45g",
            keywords = "sabritas, papas, fritas, clasicas, botana, 45g",
            detectionKeywords = "sabritas,lays,chips,potato,snack,bag,crisp"
        ),
        Product(
            id = "sabritas-limoncito-45g",
            name = "Sabritas Limoncito 45g",
            brand = ProductBrand.SABRITAS.displayName,
            category = ProductCategory.SNACKS.displayName,
            subcategory = "Papas Fritas",
            description = "Papas fritas con sabor a limón y chile",
            price = 18.00,
            barcode = "7501234567903",
            weight = "45g",
            keywords = "sabritas, limoncito, limon, chile, papas, 45g",
            detectionKeywords = "sabritas,lays,chips,lime,chili,snack,bag"
        ),

        // ===== DORITOS =====
        Product(
            id = "doritos-nacho-62g",
            name = "Doritos Nacho 62g",
            brand = ProductBrand.DORITOS.displayName,
            category = ProductCategory.SNACKS.displayName,
            subcategory = "Tortillas Chips",
            description = "Totopos de maíz con sabor a queso nacho",
            price = 22.00,
            barcode = "7501234567904",
            weight = "62g",
            keywords = "doritos, nacho, queso, totopos, maiz, 62g",
            detectionKeywords = "doritos,nacho,cheese,chips,corn,snack,bag,tortilla"
        ),
        Product(
            id = "doritos-flamas-62g",
            name = "Doritos Flamas 62g",
            brand = ProductBrand.DORITOS.displayName,
            category = ProductCategory.SNACKS.displayName,
            subcategory = "Tortillas Chips",
            description = "Totopos de maíz con chile y limón",
            price = 22.00,
            barcode = "7501234567905",
            weight = "62g",
            keywords = "doritos, flamas, chile, limon, totopos, 62g",
            detectionKeywords = "doritos,flamas,chili,lime,chips,corn,snack,bag"
        ),

        // ===== CHEETOS =====
        Product(
            id = "cheetos-torciditos-45g",
            name = "Cheetos Torciditos 45g",
            brand = ProductBrand.CHEETOS.displayName,
            category = ProductCategory.SNACKS.displayName,
            subcategory = "Snacks de Queso",
            description = "Snacks de maíz con sabor a queso en forma torcida",
            price = 18.00,
            barcode = "7501234567906",
            weight = "45g",
            keywords = "cheetos, torciditos, queso, maiz, botana, 45g",
            detectionKeywords = "cheetos,cheese,corn,snack,bag,twisted"
        ),
        Product(
            id = "cheetos-bolitas-45g",
            name = "Cheetos Bolitas 45g",
            brand = ProductBrand.CHEETOS.displayName,
            category = ProductCategory.SNACKS.displayName,
            subcategory = "Snacks de Queso",
            description = "Bolitas de maíz con sabor a queso",
            price = 18.00,
            barcode = "7501234567907",
            weight = "45g",
            keywords = "cheetos, bolitas, queso, maiz, botana, 45g",
            detectionKeywords = "cheetos,cheese,corn,snack,bag,balls"
        ),

        // ===== RUFFLES =====
        Product(
            id = "ruffles-queso-45g",
            name = "Ruffles Queso 45g",
            brand = ProductBrand.RUFFLES.displayName,
            category = ProductCategory.SNACKS.displayName,
            subcategory = "Papas Fritas",
            description = "Papas fritas onduladas con sabor a queso",
            price = 18.00,
            barcode = "7501234567908",
            weight = "45g",
            keywords = "ruffles, queso, papas, onduladas, botana, 45g",
            detectionKeywords = "ruffles,cheese,chips,potato,snack,bag,ridged"
        ),

        // ===== FRITOS =====
        Product(
            id = "fritos-originales-45g",
            name = "Fritos Originales 45g",
            brand = ProductBrand.FRITOS.displayName,
            category = ProductCategory.SNACKS.displayName,
            subcategory = "Chips de Maíz",
            description = "Chips de maíz original crujientes",
            price = 18.00,
            barcode = "7501234567909",
            weight = "45g",
            keywords = "fritos, originales, maiz, chips, crujientes, 45g",
            detectionKeywords = "fritos,corn,chips,original,snack,bag"
        ),

        // ===== TOSTITOS =====
        Product(
            id = "tostitos-tradicionales-200g",
            name = "Tostitos Tradicionales 200g",
            brand = ProductBrand.TOSTITOS.displayName,
            category = ProductCategory.SNACKS.displayName,
            subcategory = "Tortillas Chips",
            description = "Totopos de maíz tradicionales ideales para compartir",
            price = 45.00,
            barcode = "7501234567910",
            weight = "200g",
            keywords = "tostitos, tradicionales, totopos, compartir, 200g",
            detectionKeywords = "tostitos,tortilla,chips,corn,snack,bag,traditional"
        ),

        // ===== QUAKER =====
        Product(
            id = "quaker-avena-1kg",
            name = "Quaker Avena 1kg",
            brand = ProductBrand.QUAKER.displayName,
            category = ProductCategory.CEREALES.displayName,
            subcategory = "Avena",
            description = "Avena integral 100% natural rica en fibra",
            price = 65.00,
            barcode = "7501234567911",
            weight = "1kg",
            keywords = "quaker, avena, integral, natural, fibra, 1kg",
            detectionKeywords = "quaker,oats,cereal,oatmeal,breakfast,package"
        ),
        Product(
            id = "quaker-avena-500g",
            name = "Quaker Avena 500g",
            brand = ProductBrand.QUAKER.displayName,
            category = ProductCategory.CEREALES.displayName,
            subcategory = "Avena",
            description = "Avena integral 100% natural en presentación familiar",
            price = 35.00,
            barcode = "7501234567912",
            weight = "500g",
            keywords = "quaker, avena, integral, natural, fibra, 500g",
            detectionKeywords = "quaker,oats,cereal,oatmeal,breakfast,package"
        ),

        // ===== GAMESA =====
        Product(
            id = "gamesa-emperador-chocolate",
            name = "Emperador Chocolate",
            brand = ProductBrand.EMPERADOR.displayName,
            category = ProductCategory.ALIMENTOS.displayName,
            subcategory = "Galletas",
            description = "Galletas cubiertas de chocolate, el clásico de siempre",
            price = 15.00,
            barcode = "7501234567913",
            keywords = "emperador, chocolate, galletas, gamesa, clasico",
            detectionKeywords = "emperador,chocolate,cookies,biscuits,gamesa,package"
        ),
        Product(
            id = "gamesa-emperador-limon",
            name = "Emperador Limón",
            brand = ProductBrand.EMPERADOR.displayName,
            category = ProductCategory.ALIMENTOS.displayName,
            subcategory = "Galletas",
            description = "Galletas con sabor a limón, frescas y deliciosas",
            price = 15.00,
            barcode = "7501234567914",
            keywords = "emperador, limon, galletas, gamesa, fresco",
            detectionKeywords = "emperador,lemon,lime,cookies,biscuits,gamesa,package"
        ),

        // ===== H2OH! =====
        Product(
            id = "h2oh-limon-600ml",
            name = "H2OH! Limón 600ml",
            brand = ProductBrand.H2OH.displayName,
            category = ProductCategory.BEBIDAS.displayName,
            subcategory = "Agua Saborizada",
            description = "Agua con sabor a limón sin calorías",
            price = 15.00,
            barcode = "7501234567915",
            weight = "600ml",
            keywords = "h2oh, limon, agua, saborizada, sin calorias, 600ml",
            detectionKeywords = "h2oh,flavored water,lime,lemon,beverage,bottle"
        ),

        // ===== MANZANITA SOL =====
        Product(
            id = "manzanita-sol-600ml",
            name = "Manzanita Sol 600ml",
            brand = ProductBrand.MANZANITA_SOL.displayName,
            category = ProductCategory.BEBIDAS.displayName,
            subcategory = "Refrescos de Manzana",
            description = "Refresco sabor manzana con el auténtico sabor mexicano",
            price = 18.50,
            barcode = "7501234567916",
            weight = "600ml",
            keywords = "manzanita sol, manzana, refresco, mexicano, 600ml",
            detectionKeywords = "manzanita sol,manzanita,apple,soda,beverage,bottle,mexican"
        ),

        // ===== PEPSI BLACK =====
        Product(
            id = "pepsi-black-355ml",
            name = "Pepsi Black 355ml",
            brand = ProductBrand.PEPSI.displayName,
            category = ProductCategory.BEBIDAS.displayName,
            subcategory = "Refrescos de Cola",
            description = "Pepsi Black sin azúcar con sabor intenso",
            price = 16.00,
            barcode = "7501234567917",
            weight = "355ml",
            keywords = "pepsi black, cola, sin azucar, refresco, bebida, 355ml",
            detectionKeywords = "pepsi black,pepsi,cola,black,soda,beverage,drink,can"
        ),
        Product(
            id = "pepsi-black-600ml",
            name = "Pepsi Black 600ml",
            brand = ProductBrand.PEPSI.displayName,
            category = ProductCategory.BEBIDAS.displayName,
            subcategory = "Refrescos de Cola",
            description = "Pepsi Black sin azúcar en botella de 600ml",
            price = 19.00,
            barcode = "7501234567918",
            weight = "600ml",
            keywords = "pepsi black, cola, sin azucar, refresco, bebida, 600ml, botella",
            detectionKeywords = "pepsi black,pepsi,cola,black,soda,beverage,drink,bottle"
        ),
        Product(
            id = "pepsi-black-2l",
            name = "Pepsi Black 2L",
            brand = ProductBrand.PEPSI.displayName,
            category = ProductCategory.BEBIDAS.displayName,
            subcategory = "Refrescos de Cola",
            description = "Pepsi Black sin azúcar familiar de 2 litros",
            price = 38.00,
            barcode = "7501234567919",
            weight = "2L",
            keywords = "pepsi black, cola, sin azucar, refresco, familiar, 2L, botella",
            detectionKeywords = "pepsi black,pepsi,cola,black,soda,beverage,drink,bottle,big"
        ),

        // ===== SQUIRT =====
        Product(
            id = "squirt-600ml",
            name = "Squirt 600ml", 
            brand = "Squirt",
            category = ProductCategory.BEBIDAS.displayName,
            subcategory = "Refrescos Cítricos",
            description = "Refresco sabor toronja con un toque cítrico refrescante",
            price = 17.50,
            barcode = "7501234567918",
            weight = "600ml",
            keywords = "squirt, toronja, citrico, refresco, refrescante, 600ml",
            detectionKeywords = "squirt,grapefruit,citrus,soda,beverage,drink,bottle"
        )
    )

    fun getAllStores(): List<Store> = listOf(
        Store(
            customerNumber = "2003863307",
            chainLevel1 = "Independent/Small Chain",
            chainLevel2 = "Independent/Small Chain",
            customerName = "\"New\" Canton Var."
        ),
        Store(
            customerNumber = "2004897758",
            chainLevel1 = "Hra",
            chainLevel2 = "Hra",
            customerName = "\"New\" Mega Star 2"
        ),
        Store(
            customerNumber = "2004897622",
            chainLevel1 = "Customers Wo Assignment",
            chainLevel2 = "",
            customerName = "\"New\" Mega Star 3"
        ),
        Store(
            customerNumber = "2004897514",
            chainLevel1 = "Hra",
            chainLevel2 = "Hra",
            customerName = "\"New\" Mega Star 7"
        ),
        Store(
            customerNumber = "2004824931",
            chainLevel1 = "Alliance Group",
            chainLevel2 = "Active 1",
            customerName = "\"The Market Of Perham\""
        ),
        Store(
            customerNumber = "2003757032",
            chainLevel1 = "Petsmart",
            chainLevel2 = "Petsmart",
            customerName = "# 154 Petsmart East Mesa"
        ),
        Store(
            customerNumber = "2003751091",
            chainLevel1 = "Independent/Small Chain",
            chainLevel2 = "Independent/Small Chain",
            customerName = "# Food Store"
        ),
        Store(
            customerNumber = "2003620888",
            chainLevel1 = "Ner Ib Chain",
            chainLevel2 = "Cda",
            customerName = "#1 Ac Deli & Food Mart"
        ),
        Store(
            customerNumber = "2003584375",
            chainLevel1 = "Ubc",
            chainLevel2 = "Ubc",
            customerName = "#1 Airline Food (Ibc)"
        ),
        Store(
            customerNumber = "2003783703",
            chainLevel1 = "Se Asian American Assoc",
            chainLevel2 = "Se Asian A",
            customerName = "#1 Convenient Mart"
        ),
        Store(
            customerNumber = "2003611051",
            chainLevel1 = "Sunbelt Merchant Group",
            chainLevel2 = "Sunbelt Me",
            customerName = "#1 C-Store"
        ),
        Store(
            customerNumber = "2003714803",
            chainLevel1 = "Se Asian American Assoc",
            chainLevel2 = "Se Asian A",
            customerName = "#1 Discount Beverage"
        ),
        Store(
            customerNumber = "2003608436",
            chainLevel1 = "Nw Cs Independen T",
            chainLevel2 = "Nw Cs Inde",
            customerName = "#1 Food 4 Mart"
        ),
        Store(
            customerNumber = "2003504007",
            chainLevel1 = "Independent/Small Chain",
            chainLevel2 = "Independent/Small Chain",
            customerName = "#1 Food Store"
        ),
        Store(
            customerNumber = "2003818848",
            chainLevel1 = "Ibc Buyers Co-Op",
            chainLevel2 = "Active",
            customerName = "#1 Food Store"
        ),
        Store(
            customerNumber = "2003823135",
            chainLevel1 = "Independent/Small Chain",
            chainLevel2 = "Independent/Small Chain",
            customerName = "#1 Food Store"
        )
    )
}