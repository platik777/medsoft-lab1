import { defineConfig } from 'vite'
// @ts-ignore
import react from '@vitejs/plugin-react'

export default defineConfig({
    plugins: [react()],
    server: {
        port: 3000, // для reception-ui используйте 3000, для hospital-chief-ui - 3001
        host: true
    }
})