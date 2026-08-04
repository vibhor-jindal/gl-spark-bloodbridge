import { Route, Routes } from "react-router-dom";
import Navbar from "./components/Navbar";
import ProtectedRoute from "./components/ProtectedRoute";
import Home from "./pages/Home";
import Login from "./pages/Login";
import Register from "./pages/Register";
import DonorRegister from "./pages/DonorRegister";
import DonorAlerts from "./pages/DonorAlerts";
import CreateRequest from "./pages/CreateRequest";
import MyRequests from "./pages/MyRequests";
import TrackRequest from "./pages/TrackRequest";
import BloodBanks from "./pages/BloodBanks";
import BankPortal from "./pages/BankPortal";
import BankRequests from "./pages/BankRequests";
import AdminDashboard from "./pages/AdminDashboard";
import AdminManage from "./pages/AdminManage";
import Notifications from "./pages/Notifications";
import Rewards from "./pages/Rewards";

export default function App() {
  return (
    <div className="min-h-screen bg-bg">
      <Navbar />
      <Routes>
        <Route path="/" element={<Home />} />
        <Route path="/login" element={<Login />} />
        <Route path="/register" element={<Register />} />

        <Route path="/donor/register" element={
          <ProtectedRoute allowedRoles={["DONOR"]}><DonorRegister /></ProtectedRoute>
        } />
        <Route path="/donor/alerts" element={
          <ProtectedRoute allowedRoles={["DONOR"]}><DonorAlerts /></ProtectedRoute>
        } />
        <Route path="/rewards" element={
          <ProtectedRoute allowedRoles={["DONOR"]}><Rewards /></ProtectedRoute>
        } />

        <Route path="/requests/new" element={
          <ProtectedRoute allowedRoles={["REQUESTER"]}><CreateRequest /></ProtectedRoute>
        } />
        <Route path="/requests" element={
          <ProtectedRoute allowedRoles={["REQUESTER"]}><MyRequests /></ProtectedRoute>
        } />
        <Route path="/banks" element={
          <ProtectedRoute allowedRoles={["REQUESTER"]}><BloodBanks /></ProtectedRoute>
        } />

        <Route path="/requests/:requestId" element={
          <ProtectedRoute><TrackRequest /></ProtectedRoute>
        } />

        <Route path="/bank/portal" element={
          <ProtectedRoute allowedRoles={["BLOOD_BANK"]}><BankPortal /></ProtectedRoute>
        } />
        <Route path="/bank/requests" element={
          <ProtectedRoute allowedRoles={["BLOOD_BANK"]}><BankRequests /></ProtectedRoute>
        } />

        <Route path="/notifications" element={
          <ProtectedRoute><Notifications /></ProtectedRoute>
        } />

        <Route path="/admin/dashboard" element={
          <ProtectedRoute allowedRoles={["ADMIN"]}><AdminDashboard /></ProtectedRoute>
        } />
        <Route path="/admin/manage" element={
          <ProtectedRoute allowedRoles={["ADMIN"]}><AdminManage /></ProtectedRoute>
        } />
      </Routes>
    </div>
  );
}
