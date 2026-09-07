const LAN_HOST = "10.43.122.12"
const CURRENT_ENV = "device"

const ENV = {
  dev: {
    baseUrl: "http://localhost:8080",
    assetBaseUrl: "http://localhost:9000/pingan-banzu",
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
