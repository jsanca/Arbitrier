import React from 'react';

interface CreditSummaryProps {
  availableCredit: number;
  creditLimit: number;
}

export const CreditSummary: React.FC<CreditSummaryProps> = ({ availableCredit, creditLimit }) => {
  const usedCredit = Math.max(0, creditLimit - availableCredit);
  const usedPercentage = creditLimit > 0 ? (usedCredit / creditLimit) * 100 : 0;
  const isCritical = availableCredit < creditLimit * 0.15;

  const formatCurrency = (val: number) => {
    return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(val);
  };

  return (
    <div className="bg-surface-container-lowest border border-outline-variant p-6 rounded-xl shadow-sm" data-testid="credit-summary">
      <div className="flex justify-between items-center mb-4">
        <div>
          <p className="text-xs font-semibold text-secondary uppercase tracking-wider">Available Company Credit</p>
          <h3 className={`text-2xl font-bold tracking-tight mt-1 ${isCritical ? 'text-error' : 'text-primary'}`}>
            {formatCurrency(availableCredit)}
          </h3>
        </div>
        <div className="text-right">
          <p className="text-xs font-semibold text-secondary uppercase tracking-wider">Total Credit Limit</p>
          <p className="text-lg font-semibold text-on-surface mt-1">{formatCurrency(creditLimit)}</p>
        </div>
      </div>

      {/* Progress Bar */}
      <div className="w-full h-3 bg-surface-container-high rounded-full overflow-hidden">
        <div
          className={`h-full transition-all duration-500 ${isCritical ? 'bg-error animate-pulse' : 'bg-primary'}`}
          style={{ width: `${Math.min(100, Math.max(0, 100 - usedPercentage))}%` }}
        />
      </div>

      <div className="flex justify-between items-center mt-2 text-xs text-on-surface-variant">
        <span>{formatCurrency(availableCredit)} available</span>
        <span>{formatCurrency(usedCredit)} utilized ({usedPercentage.toFixed(0)}%)</span>
      </div>

      {isCritical && (
        <div className="mt-4 flex items-center gap-2 text-xs text-error bg-error-container/30 border border-error-container p-2.5 rounded-lg">
          <span className="material-symbols-outlined text-[16px]">warning</span>
          <span>Critical: Available credit limit is below 15%. Contact billing for limit extension.</span>
        </div>
      )}
    </div>
  );
};
