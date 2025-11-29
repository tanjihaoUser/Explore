<template>
  <div class="login_main">
    <div class="login_form">
      <div class="form-wrapper">
        <h3>欢迎注册博客系统</h3>
        <el-form v-model="signUpData">
          <el-form-item prop="username">
            <el-input v-model="signUpData.username" placeholder="请输入用户名">
              <template #prefix>
                <el-icon class="el-input__icon"><user /></el-icon>
              </template>
            </el-input>
          </el-form-item>
          <el-form-item prop="email">
            <el-input v-model="signUpData.email" placeholder="请输入邮箱">
              <template #prefix>
                <el-icon class="el-input__icon"><Message /></el-icon>
              </template>
            </el-input>
          </el-form-item>
          <el-form-item prop="password">
            <el-input
              v-model="signUpData.password"
              type="password"
              placeholder="请输入密码"
            >
              <template #prefix>
                <el-icon class="el-input__icon"><lock /></el-icon>
              </template>
            </el-input>
          </el-form-item>
          <el-form-item>
            <el-input
              v-model="verPassword"
              type="password"
              placeholder="确认密码"
            >
              <template #prefix>
                <el-icon class="el-input__icon"><lock /></el-icon>
              </template>
            </el-input>
          </el-form-item>
          <el-form-item>
            <el-input v-model="signUpData.phone" placeholder="请输入手机号（可选）">
              <template #prefix>
                <el-icon class="el-input__icon"><Phone /></el-icon>
              </template>
            </el-input>
          </el-form-item>
          <div class="button">
            <el-button type="primary" @click="handlesignUpData">注册</el-button>
            <el-button type="info" @click="router.push('/login')">返回</el-button>
          </div>
        </el-form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import { register } from "@/api/user.js";
import { nextTick } from "vue";

const router = useRouter();
// 注意这里要设置为响应式数据
const signUpData = ref({
  username: "",
  email: "",
  password: "",
  phone: "",
});
const verPassword = ref("");

function handlesignUpData() {
  if (signUpData.value.username == "") {
    ElMessage.error("用户名不能为空");
  } else if (signUpData.value.email == "") {
    ElMessage.error("邮箱不能为空");
  } else if (signUpData.value.password == "") {
    ElMessage.error("密码不能为空");
  } else if (signUpData.value.password != verPassword.value) {
    ElMessage.error("密码与确认密码不一致");
  } else {
    register(signUpData.value)
      .then((response) => {
        let res = response.data;
        if (res.code == 200) {
          ElMessage.success("注册成功，请登录");
          // 使用nextTick确保消息显示后再跳转
          nextTick(() => {
            router.push("/login");
          });
        }
      })
      .catch(() => {});
  }
}
</script>

<style>
.login_main {
  /* 这里必须使用url方法包裹起来 */
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
  padding: 20px 30px;
  border-radius: 12px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
  box-sizing: border-box;
  /* 注册页面字段较多，允许滚动 */
  overflow-y: auto;
}
/* 表单包装器：包含标题、输入框和按钮的整体 */
.form-wrapper {
  width: 80%;
  height: 50%;
  display: flex;
  flex-direction: column;
  justify-content: flex-start;
  align-items: center;
  margin: 0 auto;
  min-height: 0;
}
.form-wrapper h3 {
  font-size: 26px;
  font-weight: 600;
  margin-bottom: 12px;
  margin-top: 0;
  color: #333;
  text-align: center;
  flex-shrink: 0;
}
.form-wrapper .el-form {
  width: 100%;
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: flex-start;
  min-height: 0;
  overflow-y: auto;
}
.el-form-item {
  margin-bottom: 14px;
  flex-shrink: 0;
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
  flex-shrink: 0;
}
.el-button {
  flex: 1;
  height: 44px;
  font-size: 16px;
  font-weight: 500;
}
</style>
