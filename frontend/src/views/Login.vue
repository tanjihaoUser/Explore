<template>
  <div class="login_main">
    <div class="login_form">
      <div class="form-wrapper">
        <h3>欢迎登录博客系统</h3>
        <el-form v-model="loginData">
          <el-form-item>
            <el-input v-model="loginData.username" placeholder="请输入账号">
              <!-- 使用prefix-icon不一定有用，也可以使用插槽-->
              <!-- 插槽的名称固定，prefix表示前置图标，suffix表示后置图标-->
              <template #prefix>
                <el-icon class="el-input__icon"><user /></el-icon>
              </template>
            </el-input>
          </el-form-item>
          <el-form-item>
            <el-input
              v-model="loginData.password"
              type="password"
              placeholder="请输入密码"
            >
              <template #prefix>
                <el-icon class="el-input__icon"><lock /></el-icon>
              </template>
            </el-input>
          </el-form-item>
          <div class="button">
            <el-button type="primary" @click="handleLogin">登录</el-button>
            <el-button type="info" @click="router.push('/register')"
              >注册</el-button
            >
          </div>
        </el-form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { login } from "@/api/session.js";
// 使用pinia进行数据共享：先导入依赖，构建本地对象，通过set方法设置值
import { useUserStore } from "@/stores/user.js";
import { useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import { nextTick } from "vue";

// 跳转路由只需要使用vue-router下的useRouter方法，会自动寻找router/index.js进行路由
const router = useRouter();
const userStore = useUserStore();

// 注意这里要设置为响应式数据
const loginData = ref({
  username: "",
  password: "",
});

function handleLogin() {
  if (loginData.value.username == "" || loginData.value.password == "") {
    ElMessage.error("用户名或密码不能为空");
    return;
  }
  // 注意这里要加上.value
  login(loginData.value.username, loginData.value.password)
    .then((response) => {
      // 后端返回格式：{success: true, data: {...}, sessionId: "..."}
      // 或者经过拦截器转换后：{code: 200, data: {...}, ...}
      let res = response.data;
      console.log("登录响应数据:", res);
      
      // 兼容两种响应格式：code 或 success
      const isSuccess = (res.code === 200) || (res.code === null && res.success === true) || res.success === true;
      
      if (isSuccess) {
        // 注意这里
        // data中的userId和username才是具体的数据
        const sessionData = res.data;
        if (!sessionData) {
          ElMessage.error("登录失败：响应数据格式错误");
          console.error("登录响应数据:", res);
          return;
        }
        
        const userId = sessionData.userId;
        const username = sessionData.username;
        // sessionId可能在extraFields中，也可能直接在响应根级别
        const sessionId = res.sessionId || res.extraFields?.sessionId || sessionData.sessionId;
        
        if (!sessionId) {
          ElMessage.error("登录失败：未获取到会话ID");
          console.error("登录响应数据:", res);
          return;
        }
        
        if (!userId || !username) {
          ElMessage.error("登录失败：未获取到用户信息");
          console.error("登录响应数据:", res);
          return;
        }
        
        // 设置用户信息
        userStore.setUserInfo(userId, username, sessionId);
        console.log("用户信息已设置:", { userId, username, sessionId, isLoggedIn: userStore.isLoggedIn });
        
        ElMessage.success("登录成功");
        
        // 使用nextTick确保状态更新后再跳转
        nextTick(() => {
          console.log("准备跳转到 /home, 当前登录状态:", userStore.isLoggedIn);
          router.push("/home").then(() => {
            console.log("跳转成功");
          }).catch((err) => {
            console.error("跳转失败:", err);
            // 如果跳转失败，尝试使用replace
            if (err.name !== 'NavigationDuplicated') {
              router.replace("/home").catch((replaceErr) => {
                console.error("replace也失败:", replaceErr);
                ElMessage.error("页面跳转失败，请手动刷新");
              });
            }
          });
        });
      } else {
        ElMessage.error(res.message || "登录失败");
      }
    })
    // 由于可能抛出异常，需要捕获，消息提示在全局处理器中
    .catch((error) => {
      console.error("登录请求失败:", error);
      // 错误消息已在全局处理器中显示
    });
}
</script>

<style>
.login_main {
  /* 这里必须使用url方法包裹起来。不同页面之间只能使用同一张背景图，不能更换，很奇怪 */
  background-image: url("@/assets/3.png");
  display: flex;
  height: 100%;
  /* 子元素靠右显示 */
  justify-content: flex-end;
  align-items: center;
}
.login_form {
  /* 如果居中不起作用，需要加上这一句*/
  display: flex;
  background-color: rgb(157, 226, 238);
  /* 设置不同元素之间换行显示*/
  flex-direction: column;
  margin-right: 10%;
  /* 保持第一版背景框大小 */
  height: 50%;
  width: 30%;
  align-items: center;
  justify-content: center;
  padding: 25px 30px;
  border-radius: 12px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
  box-sizing: border-box;
}
/* 表单包装器：包含标题、输入框和按钮的整体 */
.form-wrapper {
  width: 80%;
  height: 50%;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  margin: 0 auto;
}
.form-wrapper h3 {
  font-size: 26px;
  font-weight: 600;
  margin-bottom: 12px;
  margin-top: 0;
  color: #333;
  text-align: center;
}
.form-wrapper .el-form {
  width: 100%;
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
}
.el-form-item {
  margin-bottom: 16px;
}
.el-input {
  height: 44px;
  font-size: 16px;
}
.el-input__inner {
  height: 44px;
  font-size: 16px;
  padding: 0 15px 0 45px;
  line-height: 44px;
}
.el-input__prefix {
  left: 12px;
}
.el-icon {
  font-size: 20px;
}
.button {
  width: 100%;
  display: flex;
  gap: 16px;
  margin-top: 10px;
}
.el-button {
  flex: 1;
  height: 44px;
  font-size: 16px;
  font-weight: 500;
}
</style>
