import { Link } from 'react-router-dom';

export default function NotFoundPage() {
  return (
    <div className="flex min-h-[calc(100vh-4rem)] flex-col items-center justify-center px-4 text-center">
      <h1 className="text-7xl font-bold text-primary-600">404</h1>
      <p className="mt-4 text-xl font-semibold text-gray-900">Page not found</p>
      <p className="mt-2 text-gray-500">The page you're looking for doesn't exist or has been moved.</p>
      <Link
        to="/"
        className="mt-6 rounded-md bg-primary-600 px-5 py-2.5 text-sm font-semibold text-white hover:bg-primary-700 transition-colors"
      >
        Back to Home
      </Link>
    </div>
  );
}
