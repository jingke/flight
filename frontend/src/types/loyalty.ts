export interface LoyaltyPoints {
  id: number;
  user_id: number;
  earned: number;
  redeemed: number;
  balance: number;
}

export interface LoyaltyRedeem {
  points: number;
}
