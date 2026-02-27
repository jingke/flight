import { useState, useEffect } from 'react';
import toast from 'react-hot-toast';
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from 'recharts';
import Spinner from '@/components/ui/Spinner';
import * as loyaltyService from '@/services/loyalty.service';
import type { LoyaltyPoints } from '@/types';

export default function LoyaltyPage() {
  const [loyalty, setLoyalty] = useState<LoyaltyPoints | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [redeemAmount, setRedeemAmount] = useState('');
  const [isRedeeming, setIsRedeeming] = useState(false);

  useEffect(() => {
    loadLoyalty();
  }, []);

  async function loadLoyalty() {
    setIsLoading(true);
    try {
      const data = await loyaltyService.getLoyalty();
      setLoyalty(data);
    } catch {
      setLoyalty(null);
    } finally {
      setIsLoading(false);
    }
  }

  async function handleRedeem(e: React.FormEvent) {
    e.preventDefault();
    const points = Number(redeemAmount);
    if (!points || points <= 0) {
      toast.error('Enter a valid number of points');
      return;
    }
    if (loyalty && points > loyalty.balance) {
      toast.error('Insufficient points');
      return;
    }
    setIsRedeeming(true);
    try {
      const updated = await loyaltyService.redeemPoints({ points });
      setLoyalty(updated);
      setRedeemAmount('');
      toast.success(`${points} points redeemed successfully!`);
    } catch {
      toast.error('Failed to redeem points');
    } finally {
      setIsRedeeming(false);
    }
  }

  if (isLoading) {
    return (
      <div className="flex justify-center py-24">
        <Spinner className="h-10 w-10" />
      </div>
    );
  }

  const chartData = loyalty
    ? [
        { name: 'Earned', value: loyalty.earned, fill: '#22c55e' },
        { name: 'Redeemed', value: loyalty.redeemed, fill: '#f59e0b' },
        { name: 'Balance', value: loyalty.balance, fill: '#2563eb' },
      ]
    : [];

  return (
    <div className="mx-auto max-w-5xl px-4 py-8 sm:px-6 lg:px-8">
      <div className="mb-8">
        <h1 className="text-3xl font-bold text-gray-900">Loyalty Dashboard</h1>
        <p className="mt-1 text-gray-500">Track your points, view history, and redeem rewards.</p>
      </div>

      {!loyalty ? (
        <div className="rounded-xl border-2 border-dashed border-gray-200 py-16 text-center">
          <svg className="mx-auto h-12 w-12 text-gray-300" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M12 8v13m0-13V6a2 2 0 112 2h-2zm0 0V5.5A2.5 2.5 0 109.5 8H12zm-7 4h14M5 12a2 2 0 110-4h14a2 2 0 110 4M5 12v7a2 2 0 002 2h10a2 2 0 002-2v-7" />
          </svg>
          <h3 className="mt-3 text-sm font-semibold text-gray-900">No loyalty data yet</h3>
          <p className="mt-1 text-sm text-gray-500">Book flights to start earning loyalty points!</p>
        </div>
      ) : (
        <>
          {/* Points Cards */}
          <div className="mb-8 grid gap-4 sm:grid-cols-3">
            <div className="rounded-xl border border-gray-200 bg-white p-6 shadow-sm">
              <div className="flex items-center gap-3">
                <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-green-100">
                  <svg className="h-5 w-5 text-green-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 10l7-7m0 0l7 7m-7-7v18" />
                  </svg>
                </div>
                <div>
                  <p className="text-sm text-gray-500">Total Earned</p>
                  <p className="text-2xl font-bold text-green-600">{loyalty.earned.toLocaleString()}</p>
                </div>
              </div>
            </div>
            <div className="rounded-xl border border-gray-200 bg-white p-6 shadow-sm">
              <div className="flex items-center gap-3">
                <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-amber-100">
                  <svg className="h-5 w-5 text-amber-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8c-1.657 0-3 .895-3 2s1.343 2 3 2 3 .895 3 2-1.343 2-3 2m0-8c1.11 0 2.08.402 2.599 1M12 8V7m0 1v8m0 0v1m0-1c-1.11 0-2.08-.402-2.599-1M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                  </svg>
                </div>
                <div>
                  <p className="text-sm text-gray-500">Redeemed</p>
                  <p className="text-2xl font-bold text-amber-600">{loyalty.redeemed.toLocaleString()}</p>
                </div>
              </div>
            </div>
            <div className="rounded-xl border border-gray-200 bg-gradient-to-br from-primary-50 to-white p-6 shadow-sm ring-1 ring-primary-100">
              <div className="flex items-center gap-3">
                <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-primary-100">
                  <svg className="h-5 w-5 text-primary-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M11.049 2.927c.3-.921 1.603-.921 1.902 0l1.519 4.674a1 1 0 00.95.69h4.915c.969 0 1.371 1.24.588 1.81l-3.976 2.888a1 1 0 00-.363 1.118l1.518 4.674c.3.922-.755 1.688-1.538 1.118l-3.976-2.888a1 1 0 00-1.176 0l-3.976 2.888c-.783.57-1.838-.197-1.538-1.118l1.518-4.674a1 1 0 00-.363-1.118l-3.976-2.888c-.784-.57-.38-1.81.588-1.81h4.914a1 1 0 00.951-.69l1.519-4.674z" />
                  </svg>
                </div>
                <div>
                  <p className="text-sm text-gray-500">Available Balance</p>
                  <p className="text-2xl font-bold text-primary-600">{loyalty.balance.toLocaleString()}</p>
                </div>
              </div>
            </div>
          </div>

          <div className="grid gap-6 lg:grid-cols-5">
            {/* Chart */}
            <div className="lg:col-span-3 rounded-xl border border-gray-200 bg-white p-6 shadow-sm">
              <h2 className="mb-4 text-lg font-semibold text-gray-900">Points Overview</h2>
              <div className="h-64">
                <ResponsiveContainer width="100%" height="100%">
                  <BarChart data={chartData} barCategoryGap="30%">
                    <CartesianGrid strokeDasharray="3 3" stroke="#f0f0f0" />
                    <XAxis dataKey="name" tick={{ fontSize: 13 }} />
                    <YAxis tick={{ fontSize: 13 }} />
                    <Tooltip
                      contentStyle={{ borderRadius: '8px', border: '1px solid #e5e7eb', fontSize: '13px' }}
                    />
                    <Bar dataKey="value" radius={[6, 6, 0, 0]} />
                  </BarChart>
                </ResponsiveContainer>
              </div>
            </div>

            {/* Redeem */}
            <div className="lg:col-span-2 rounded-xl border border-gray-200 bg-white p-6 shadow-sm">
              <h2 className="mb-2 text-lg font-semibold text-gray-900">Redeem Points</h2>
              <p className="mb-4 text-sm text-gray-500">
                Use your loyalty points for discounts on future bookings. 100 points = $1 discount.
              </p>
              <form onSubmit={handleRedeem} className="space-y-3">
                <div>
                  <label className="mb-1.5 block text-sm font-medium text-gray-700">Points to Redeem</label>
                  <input
                    type="number"
                    min="1"
                    max={loyalty.balance}
                    value={redeemAmount}
                    onChange={(e) => setRedeemAmount(e.target.value)}
                    placeholder={`Up to ${loyalty.balance.toLocaleString()}`}
                    className="w-full rounded-lg border border-gray-300 px-3 py-2.5 text-sm focus:border-primary-500 focus:outline-none"
                  />
                  {redeemAmount && Number(redeemAmount) > 0 && (
                    <p className="mt-1 text-xs text-gray-400">
                      ≈ ${(Number(redeemAmount) / 100).toFixed(2)} discount value
                    </p>
                  )}
                </div>
                <button
                  type="submit"
                  disabled={isRedeeming || loyalty.balance === 0}
                  className="w-full rounded-lg bg-primary-600 py-2.5 text-sm font-semibold text-white hover:bg-primary-700 disabled:opacity-50 transition-colors"
                >
                  {isRedeeming ? 'Redeeming...' : 'Redeem Points'}
                </button>
              </form>

              <div className="mt-6 border-t border-gray-100 pt-4">
                <h3 className="text-sm font-medium text-gray-700">How it works</h3>
                <ul className="mt-2 space-y-1.5 text-xs text-gray-500">
                  <li className="flex items-start gap-2">
                    <span className="mt-0.5 h-1 w-1 flex-shrink-0 rounded-full bg-primary-400" />
                    Earn 10% of booking value as points
                  </li>
                  <li className="flex items-start gap-2">
                    <span className="mt-0.5 h-1 w-1 flex-shrink-0 rounded-full bg-primary-400" />
                    100 points = $1 discount
                  </li>
                  <li className="flex items-start gap-2">
                    <span className="mt-0.5 h-1 w-1 flex-shrink-0 rounded-full bg-primary-400" />
                    Points never expire
                  </li>
                </ul>
              </div>
            </div>
          </div>
        </>
      )}
    </div>
  );
}
