import axios from "axios";

const client = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || "http://localhost:8080"
});

client.interceptors.request.use((config) => {
  const raw = localStorage.getItem("bloodbridge_auth");
  if (raw) {
    const auth = JSON.parse(raw);
    config.headers.Authorization = `Bearer ${auth.token}`;
  }
  return config;
});

client.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem("bloodbridge_auth");
      window.location.href = "/login";
    }
    return Promise.reject(error);
  }
);

export default client;
