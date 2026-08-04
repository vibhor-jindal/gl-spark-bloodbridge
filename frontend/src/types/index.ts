export type Role = "DONOR" | "REQUESTER" | "BLOOD_BANK" | "ADMIN";
export type BloodGroup = "A+" | "A-" | "B+" | "B-" | "AB+" | "AB-" | "O+" | "O-";
export type Urgency = "CRITICAL" | "HIGH" | "NORMAL";
export type RequestStatus =
  | "PENDING"
  | "MATCHED"
  | "CONFIRMED"
  | "BANK_RESERVED"
  | "OUT_FOR_DELIVERY"
  | "FULFILLED"
  | "NO_DONORS_FOUND"
  | "CANCELLED";
export type FulfillmentSource = "DONOR" | "BLOOD_BANK";

export interface AuthUser {
  userId: number;
  fullName: string;
  email: string;
  role: Role;
  token: string;
  expiresInSeconds: number;
}

export interface RegisterRequest {
  fullName: string;
  email: string;
  password: string;
  role: Role;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface DonorRequest {
  name: string;
  bloodGroup: BloodGroup;
  phone?: string;
  email?: string;
  city: string;
  latitude: number;
  longitude: number;
}

export interface DonorResponse {
  donorId: number;
  userId: number;
  name: string;
  bloodGroup: BloodGroup;
  phone?: string;
  email?: string;
  city: string;
  latitude: number;
  longitude: number;
  isAvailable: boolean;
  lastDonationDate: string | null;
  eligibleToDonate: boolean;
}

export interface RequestCreateRequest {
  patientName: string;
  bloodGroup: BloodGroup;
  unitsNeeded: number;
  hospitalName: string;
  city: string;
  latitude?: number;
  longitude?: number;
  urgency: Urgency;
}

export interface RequestResponse {
  requestId: number;
  requesterId: number;
  patientName: string;
  bloodGroup: BloodGroup;
  unitsNeeded: number;
  hospitalName: string;
  city: string;
  latitude?: number;
  longitude?: number;
  urgency: Urgency;
  status: RequestStatus;
  confirmedDonorId?: number;
  fulfillmentSource?: FulfillmentSource;
  bloodBankUserId?: number;
  reservedBatchId?: number;
  otpPending?: boolean;
  /** ISO timestamp while out for delivery; null after fulfillment. */
  otpExpiresAt?: string | null;
  /** True when OUT_FOR_DELIVERY and OTP expiry is missing or in the past. */
  otpExpired?: boolean;
  createdAt: string;
}

export interface MatchResponse {
  matchId: number;
  requestId: number;
  donorId: number;
  donorName?: string;
  donorBloodGroup?: string;
  donorCity?: string;
  donorPhone?: string;
  /** Distance-based score; null/legacy 1000 means city-only (not reward points). */
  matchScore?: number | null;
  responseStatus: "PENDING" | "ACCEPTED" | "DECLINED" | "TIMED_OUT";
  createdAt: string;
}

export interface NotificationLogResponse {
  notificationId: number;
  requestId: number;
  recipientId: number;
  recipientType: "DONOR" | "REQUESTER" | "BLOOD_BANK";
  channel: "EMAIL" | "SMS" | "PUSH";
  subject: string;
  status: "SENT" | "FAILED";
  deliveredAt: string;
}

export interface InventoryRequest {
  bloodBankName: string;
  city: string;
  bloodGroup: BloodGroup;
  unitsAvailable: number;
  collectedDate: string;
  expiryDate: string;
}

export interface InventoryResponse {
  batchId: number;
  bloodBankName: string;
  ownerUserId?: number;
  city: string;
  bloodGroup: BloodGroup;
  unitsAvailable: number;
  collectedDate: string;
  expiryDate: string;
  status: "ACTIVE" | "EXPIRED" | "DEPLETED";
}

export interface ReserveResponse {
  bloodGroup: string;
  city: string;
  unitsReserved: number;
  remainingAvailable: number;
  batchId?: number;
  ownerUserId?: number;
  bloodBankName?: string;
}

export interface LowStockAlert {
  bloodGroup: string;
  city: string;
  availableUnits: number;
  threshold: number;
}

export interface DashboardResponse {
  totalRequests: number;
  fulfilledOrConfirmedCount: number;
  fulfillmentRatePercent: number;
  averageMatchTimeSeconds: number | null;
  requestsByBloodGroup: Record<string, number>;
  requestsByStatus: Record<string, number>;
}

export interface RewardProfileResponse {
  donorId: number;
  donorName?: string | null;
  city: string | null;
  totalPoints: number;
  donationCount: number;
  badges: string[];
}

export interface LeaderboardEntry {
  donorId: number;
  donorName?: string | null;
  city: string;
  totalPoints: number;
  donationCount: number;
  rank: number;
}

export interface PlatformUser {
  userId: number;
  fullName: string;
  email: string;
  role: Role;
}
