// src/entities/Product.js
export default class Product {
    // URL de l'API pour les produits
    static endpoint = 'http://localhost:8080/api/v1/products/';

    // Configuration des champs à afficher dans le formulaire et le tableau
    static fields = [
        { name: 'id', label: 'ID', type: 'number', readonly: true },
        { name: 'reference', label: 'Référence', type: 'text' },
        { name: 'unitPriceHT', label: 'Prix HT (€)', type: 'number', step: '0.01' },
        { name: 'quantityAvailable', label: 'Quantité', type: 'number' }
    ];
}
