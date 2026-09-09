const LAN_HOST = "localhost"
const CURRENT_ENV = "dev"

const ENV = {
  dev: {
    baseUrl: "http://localhost:8080",
    assetBaseUrl: "http://localhost:9000/demo-safeteam",
    uploadTimeout: 60000,
    requestTimeout: 15000
  },
  device: {
    baseUrl: `http://${LAN_HOST}:8080`,
    assetBaseUrl: `http://${LAN_HOST}:8080/api/assets`,
    uploadTimeout: 60000,
    requestTimeout: 15000
  }
}

module.exports = {
  currentEnv: CURRENT_ENV,
  lanHost: LAN_HOST,
  ...ENV[CURRENT_ENV]
}
