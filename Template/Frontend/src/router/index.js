import { createRouter, createWebHistory } from 'vue-router'
import ProductCRUD from '@/views/ProductCRUD.vue'
import OrderCRUD from '@/components/OrderCRUD.vue'

const routes = [
    { path: '/', redirect: '/products' },
    { path: '/products', component: ProductCRUD },
    { path: '/orders', component: OrderCRUD }
]

const router = createRouter({
    history: createWebHistory(),
    routes
})

export default router
