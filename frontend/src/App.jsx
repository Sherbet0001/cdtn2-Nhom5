import React from 'react';
import { Navigate, Route, Routes } from 'react-router-dom';
import Navbar from './components/Navbar';
import { useAuth } from './contexts/AuthContext';
import EmployerJobsPage from './pages/EmployerJobsPage';
import JobApplicantsPage from './pages/JobApplicantsPage';
import JobDetailPage from './pages/JobDetailPage';
import JobFormPage from './pages/JobFormPage';
import JobListPage from './pages/JobListPage';
import LoginPage from './pages/LoginPage';
import ProfilePage from './pages/ProfilePage';
import RegisterPage from './pages/RegisterPage';

const ProtectedRoute = ({ children, allowedRoles }) => {
  const { isAuthenticated, user } = useAuth();
  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }
  if (allowedRoles && user && !allowedRoles.includes(user.role)) {
    return <Navigate to="/jobs" replace />;
  }
  return children;
};

const GuestRoute = ({ children }) => {
  const { isAuthenticated, user } = useAuth();
  if (isAuthenticated) {
    if (user?.role === 'EMPLOYER') {
      return <Navigate to="/employer/jobs" replace />;
    }
    return <Navigate to="/jobs" replace />;
  }
  return children;
};

const App = () => {
  return (
    <div className="min-h-screen bg-slate-50">
      <Navbar />
      <main>
        <Routes>
          <Route path="/" element={<Navigate to="/jobs" replace />} />
          <Route path="/jobs" element={<JobListPage />} />
          <Route path="/jobs/:id" element={<JobDetailPage />} />
          <Route
            path="/login"
            element={
              <GuestRoute>
                <LoginPage />
              </GuestRoute>
            }
          />
          <Route
            path="/register"
            element={
              <GuestRoute>
                <RegisterPage />
              </GuestRoute>
            }
          />
          <Route
            path="/profile"
            element={
              <ProtectedRoute>
                <ProfilePage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/profiles/:userId"
            element={
              <ProtectedRoute>
                <ProfilePage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/employer/jobs"
            element={
              <ProtectedRoute allowedRoles={['EMPLOYER']}>
                <EmployerJobsPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/employer/jobs/new"
            element={
              <ProtectedRoute allowedRoles={['EMPLOYER']}>
                <JobFormPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/employer/jobs/:id/edit"
            element={
              <ProtectedRoute allowedRoles={['EMPLOYER']}>
                <JobFormPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/employer/jobs/:jobId/applications"
            element={
              <ProtectedRoute allowedRoles={['EMPLOYER']}>
                <JobApplicantsPage />
              </ProtectedRoute>
            }
          />
          <Route path="*" element={<Navigate to="/jobs" replace />} />
        </Routes>
      </main>
    </div>
  );
};

export default App;
