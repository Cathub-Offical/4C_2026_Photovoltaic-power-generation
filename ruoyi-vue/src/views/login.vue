<template>
  <div class="login">
    <canvas ref="solarCanvas" class="solar-canvas"></canvas>
    <!-- 中间部分form -->
    <div class="middle-view">
      <div class="left-wrapper">
        <div class="login-font">{{ "光能智界 光伏发电管理平台" }}</div>
        <img src="@/assets/images/font01.png" alt="" style="width: 380px" />
        <svg-icon icon-class="logo" class-name="solar-tech-logo" />
      </div>
      <div class="right-wrapper">
        <div class="header">
          <span>账号登录</span>
        </div>
        <el-form ref="loginRef" :model="loginForm" :rules="loginRules" class="login-form" :label-position="'top'">
          <el-form-item prop="username" label="账号">
            <el-input v-model="loginForm.username" type="text" size="large" auto-complete="off" placeholder="账号">
            </el-input>
          </el-form-item>
          <el-form-item prop="password" label="密码">
            <el-input
              v-model="loginForm.password"
              type="password"
              size="large"
              auto-complete="off"
              placeholder="密码"
              show-password
              @keyup.enter="handleLogin"
            >
            </el-input>
          </el-form-item>
          <el-form-item prop="code" v-if="captchaEnabled" label="验证码">
            <el-input
              v-model="loginForm.code"
              size="large"
              auto-complete="off"
              placeholder="验证码"
              style="width: 230px"
              @keyup.enter="handleLogin"
            >
            </el-input>
            <div class="login-code">
              <img :src="codeUrl" @click="getCode" class="login-code-img" />
            </div>
          </el-form-item>
          <el-checkbox v-model="loginForm.rememberMe" style="margin: 0px 0px 25px 0px">记住密码</el-checkbox>
          <el-form-item style="width: 100%">
            <el-button
              :loading="loading"
              size="large"
              type="primary"
              style="width: 100%"
              color="#2563eb"
              @click.prevent="handleLogin"
              class="submit-btn"
            >
              <span v-if="!loading">登 录</span>
              <span v-else>登 录 中...</span>
            </el-button>
          </el-form-item>
        </el-form>
      </div>
    </div>
    <!-- <el-form ref="loginRef" :model="loginForm" :rules="loginRules" class="login-form">
      <el-form-item prop="username">
        <el-input v-model="loginForm.username" type="text" size="large" auto-complete="off" placeholder="账号">
          <template #prefix><svg-icon icon-class="user" class="el-input__icon input-icon" /></template>
        </el-input>
      </el-form-item>
      <el-form-item prop="password">
        <el-input
          v-model="loginForm.password"
          type="password"
          size="large"
          auto-complete="off"
          placeholder="密码"
          @keyup.enter="handleLogin"
        >
          <template #prefix><svg-icon icon-class="password" class="el-input__icon input-icon" /></template>
        </el-input>
      </el-form-item>
      <el-form-item prop="code" v-if="captchaEnabled">
        <el-input
          v-model="loginForm.code"
          size="large"
          auto-complete="off"
          placeholder="验证码"
          style="width: 63%"
          @keyup.enter="handleLogin"
        >
          <template #prefix><svg-icon icon-class="validCode" class="el-input__icon input-icon" /></template>
        </el-input>
        <div class="login-code">
          <img :src="codeUrl" @click="getCode" class="login-code-img" />
        </div>
      </el-form-item>
      <el-checkbox v-model="loginForm.rememberMe" style="margin: 0px 0px 25px 0px">记住密码</el-checkbox>
      <el-form-item style="width: 100%">
        <el-button :loading="loading" size="large" type="primary" style="width: 100%" @click.prevent="handleLogin">
          <span v-if="!loading">登 录</span>
          <span v-else>登 录 中...</span>
        </el-button>
        <div style="float: right" v-if="register">
          <router-link class="link-type" :to="'/register'">立即注册</router-link>
        </div>
      </el-form-item>
    </el-form> -->
    <!--  底部  -->
    <div class="el-login-footer">
      <span>{{ "Copyright ©" + new Date().getFullYear() + " PvCloud All Rights Reserved." }}</span>
    </div>
  </div>
</template>

<script setup>
import { getCodeImg } from "@/api/login"
import Cookies from "js-cookie"
import { encrypt, decrypt } from "@/utils/jsencrypt"
import useUserStore from "@/store/modules/user"
import logo from "@/assets/icons/svg/logo.svg"

const solarCanvas = ref(null)
let animId = null

onMounted(() => {
  initSolarEffect()
})

onUnmounted(() => {
  if (animId) cancelAnimationFrame(animId)
})

function initSolarEffect() {
  const canvas = solarCanvas.value
  if (!canvas) return
  const ctx = canvas.getContext('2d')
  let w = canvas.width = window.innerWidth
  let h = canvas.height = window.innerHeight

  // 六边形网格参数
  const hexSize = 40
  const hexHeight = hexSize * 2
  const hexWidth = Math.sqrt(3) * hexSize
  const xOffset = hexWidth
  const yOffset = hexHeight * 0.75

  // 活跃的六边形（模拟光伏板发电）
  const activeHexes = []
  
  window.addEventListener('resize', () => {
    w = canvas.width = window.innerWidth
    h = canvas.height = window.innerHeight
  })

  function drawHex(x, y, r, color, fill = false) {
    ctx.beginPath()
    for (let i = 0; i < 6; i++) {
      const angle = Math.PI / 3 * i + Math.PI / 6
      const px = x + r * Math.cos(angle)
      const py = y + r * Math.sin(angle)
      i === 0 ? ctx.moveTo(px, py) : ctx.lineTo(px, py)
    }
    ctx.closePath()
    if (fill) {
      ctx.fillStyle = color
      ctx.fill()
    } else {
      ctx.strokeStyle = color
      ctx.lineWidth = 1
      ctx.stroke()
    }
  }

  function animate() {
    animId = requestAnimationFrame(animate)
    ctx.clearRect(0, 0, w, h) // 清除画布，透出CSS背景渐变

    // 1. 绘制基础网格
    const rows = Math.ceil(h / yOffset) + 1
    const cols = Math.ceil(w / xOffset) + 1

    for (let r = 0; r < rows; r++) {
      for (let c = 0; c < cols; c++) {
        const x = c * xOffset + (r % 2 === 0 ? 0 : xOffset / 2)
        const y = r * yOffset
        // 淡淡的网格线
        drawHex(x, y, hexSize - 2, 'rgba(59, 130, 246, 0.08)')
      }
    }

    // 2. 更新和绘制活跃六边形 (模拟能量)
    if (Math.random() < 0.1) { // 随机产生新激活的格子
      const r = Math.floor(Math.random() * rows)
      const c = Math.floor(Math.random() * cols)
      const x = c * xOffset + (r % 2 === 0 ? 0 : xOffset / 2)
      const y = r * yOffset
      activeHexes.push({ x, y, life: 1.0, decay: 0.01 + Math.random() * 0.02 })
    }

    for (let i = activeHexes.length - 1; i >= 0; i--) {
      const hx = activeHexes[i]
      hx.life -= hx.decay
      if (hx.life <= 0) {
        activeHexes.splice(i, 1)
        continue
      }
      
      const alpha = Math.sin(hx.life * Math.PI) * 0.6 // 呼吸效果
      // 科技蓝 + 少量光伏黄
      const color = Math.random() > 0.8 
        ? `rgba(250, 204, 21, ${alpha})` // 黄色
        : `rgba(59, 130, 246, ${alpha})` // 蓝色
      
      drawHex(hx.x, hx.y, hexSize - 2, color, true)
      
      // 发光边缘
      ctx.shadowBlur = 15
      ctx.shadowColor = color
      drawHex(hx.x, hx.y, hexSize - 2, color, false)
      ctx.shadowBlur = 0
    }
  }
  animate()
}

const userStore = useUserStore()
const route = useRoute()
const router = useRouter()
const { proxy } = getCurrentInstance()

const systemInfo1 = JSON.parse(Cookies.get("SystemInfo") || "{}")
const systemInfo = {
  ...systemInfo1,
  homeLogo: systemInfo1.homeLogo
    ? systemInfo1.homeLogo.includes("http")
      ? systemInfo1.homeLogo
      : "https://demo-ems.zhitancloud.com" + systemInfo1.homeLogo
    : "",
}
console.log(systemInfo)

const loginForm = ref({
  username: "guestUser",
  password: "guest@123456",
  rememberMe: false,
  code: "",
  uuid: "",
})

const loginRules = {
  username: [{ required: true, trigger: "blur", message: "请输入您的账号" }],
  password: [{ required: true, trigger: "blur", message: "请输入您的密码" }],
  code: [{ required: true, trigger: "change", message: "请输入验证码" }],
}

const codeUrl = ref("")
const loading = ref(false)
// 验证码开关
const captchaEnabled = ref(true)
// 注册开关
const register = ref(false)
const redirect = ref(undefined)

watch(
  route,
  (newRoute) => {
    redirect.value = newRoute.query && newRoute.query.redirect
  },
  { immediate: true }
)

function handleLogin() {
  proxy.$refs.loginRef.validate((valid) => {
    if (valid) {
      loading.value = true
      // 勾选了需要记住密码设置在 cookie 中设置记住用户名和密码
      if (loginForm.value.rememberMe) {
        Cookies.set("username", loginForm.value.username, { expires: 30 })
        Cookies.set("password", encrypt(loginForm.value.password), { expires: 30 })
        Cookies.set("rememberMe", loginForm.value.rememberMe, { expires: 30 })
      } else {
        // 否则移除
        Cookies.remove("username")
        Cookies.remove("password")
        Cookies.remove("rememberMe")
      }
      // 调用action的登录方法
      userStore
        .login(loginForm.value)
        .then(() => {
          router.push({ path: redirect.value || "/bigscreen" })
        })
        .catch(() => {
          loading.value = false
          // 重新获取验证码
          if (captchaEnabled.value) {
            getCode()
          }
        })
    }
  })
}

function getCode() {
  getCodeImg().then((res) => {
    captchaEnabled.value = res.captchaEnabled === undefined ? true : res.captchaEnabled
    if (captchaEnabled.value) {
      codeUrl.value = "data:image/gif;base64," + res.img
      loginForm.value.uuid = res.uuid
    }
  })
}

function getCookie() {
  const username = Cookies.get("username")
  const password = Cookies.get("password")
  const rememberMe = Cookies.get("rememberMe")
  loginForm.value = {
    username: username === undefined ? loginForm.value.username : username,
    password: password === undefined ? loginForm.value.password : decrypt(password),
    rememberMe: rememberMe === undefined ? false : Boolean(rememberMe),
  }
}

getCode()
getCookie()
</script>

<style lang="scss" scoped>
.login {
  display: flex;
  align-items: center;
  height: 100%;
  background: radial-gradient(ellipse at 30% 50%, #0a1628 0%, #050d1a 60%, #020810 100%);
  flex-direction: column;
  position: relative;
  min-width: 700px;
  min-height: 700px;
  overflow: hidden;
}

.solar-canvas {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  z-index: 0;
  pointer-events: none;
}

.middle-view {
  display: flex;
  align-items: center;
  justify-content: space-around;
  height: 100%;
  width: 1200px;
  z-index: 10;
  .left-wrapper {
    width: 420px;
    display: flex;
    flex-direction: column;
  }
  .login-font {
    font-size: 34px;
    font-weight: 700;
    white-space: nowrap;
    background: linear-gradient(135deg, #60a5fa 0%, #3b82f6 100%);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
    background-clip: text;
    margin-bottom: 10px;
    text-shadow: 0 0 40px rgba(59, 130, 246, 0.3);
  }
  .solar-tech-logo {
    width: 160px;
    height: 160px;
    margin-top: 20px;
  }
}

.right-wrapper {
  border-radius: 24px;
  background: rgba(10, 20, 40, 0.75);
  backdrop-filter: blur(24px);
  width: 420px;
  position: relative;
  box-shadow:
    0 25px 60px -12px rgba(0, 0, 0, 0.6),
    0 0 0 1px rgba(59, 130, 246, 0.15),
    inset 0 1px 0 rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(59, 130, 246, 0.12);
  .header {
    height: 56px;
    line-height: 56px;
    border-bottom: 1px solid rgba(59, 130, 246, 0.15);
    color: #e2e8f0;
    font-size: 18px;
    margin-bottom: 22px;
    span {
      display: inline-block;
      height: 56px;
      line-height: 62px;
      border-bottom: 3px solid;
      border-image: linear-gradient(90deg, #3b82f6, #3b82f6) 1;
      margin-left: 32px;
      font-weight: 600;
    }
  }
}

:deep(.el-input__wrapper) {
  background-color: rgba(15, 25, 50, 0.6) !important;
  border: 1px solid rgba(59, 130, 246, 0.2);
  border-radius: 8px;
  transition: all 0.3s ease;
  &:hover, &:focus-within {
    border-color: rgba(59, 130, 246, 0.5);
    box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.12);
  }
}
:deep(.el-input__inner) {
  color: #e2e8f0;
}
:deep(.el-form-item__label) {
  color: #94a3b8 !important;
  font-weight: 500;
}
:deep(.el-checkbox__label) {
  color: #94a3b8;
}
:deep(.el-checkbox__inner) {
  background-color: rgba(15, 25, 50, 0.6);
  border-color: rgba(59, 130, 246, 0.3);
}

.login-form {
  padding: 0 32px 20px;

  .submit-btn {
    width: 100%;
    height: 48px;
    background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
    border-radius: 10px;
    font-size: 16px;
    font-weight: 600;
    border: none;
    box-shadow: 0 4px 15px rgba(37, 99, 235, 0.3);
    transition: all 0.3s ease;
    &:hover {
      transform: translateY(-1px);
      background: linear-gradient(135deg, #60a5fa 0%, #3b82f6 100%);
      box-shadow: 0 0 20px rgba(250, 204, 21, 0.3), 0 4px 15px rgba(37, 99, 235, 0.4);
    }
  }

  .el-input {
    height: 44px;

    input {
      height: 44px;
    }
  }

  .input-icon {
    height: 39px;
    width: 14px;
    margin-left: 0px;
  }
}

.login-tip {
  font-size: 13px;
  text-align: center;
  color: #64748b;
}

.login-code {
  height: 44px;
  float: right;

  img {
    cursor: pointer;
    vertical-align: middle;
    border-radius: 6px;
  }
  .login-code-img {
    height: 44px;
  }
}

.login-logo-img {
  max-height: 100px;
  margin: 0 auto;
  position: absolute;
  top: 35px;
  left: 65px;
  z-index: 10;
  filter: drop-shadow(0 0 15px rgba(59, 130, 246, 0.4));
}

.el-login-footer {
  height: 60px;
  line-height: 60px;
  position: fixed;
  bottom: 0;
  width: 100%;
  text-align: center;
  color: rgba(148, 163, 184, 0.5);
  font-family: Arial;
  font-size: 14px;
  letter-spacing: 1px;
  z-index: 10;
}
</style>
