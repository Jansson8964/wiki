import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import store from './store'
import Antd from 'ant-design-vue'
import 'ant-design-vue/dist/antd.css'
import * as Icons from '@ant-design/icons-vue'
import axios from 'axios';
import {Tool} from "@/util/tool";

axios.defaults.baseURL = process.env.VUE_APP_SERVER;  // 在使用 axios 发送请求时全局的base域

/**
 * axios 拦截器
 */
axios.interceptors.request.use(function (reqConf) {
    console.log('Request Parameters：', reqConf);
    const token = store.state.localUser.token;
    if (Tool.isNotEmpty(token)) {
        reqConf.headers.token = token;
        console.log("Request headers to add token:", token);
    }
    return reqConf;
}, error => {
    return Promise.reject(error);
});

axios.interceptors.response.use(function (resp) {
    console.log('Return results：', resp);
    return resp;
}, error => {
    console.log('Return Error：', error);
    return Promise.reject(error);
})


const app = createApp(App);
app.use(store).use(router).use(Antd).mount('#app');

// 全局使用 Ant Design 的图标库
const icons: any = Icons;
for (const i in icons) {
    app.component(i, icons[i]);
}


console.log('Environment:', process.env.NODE_ENV)