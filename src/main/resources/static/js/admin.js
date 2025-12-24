// Admin JavaScript

// Auto-hide alerts after 5 seconds
document.addEventListener('DOMContentLoaded', function() {
    const alerts = document.querySelectorAll('.alert:not(.alert-permanent)');
    alerts.forEach(function(alert) {
        setTimeout(function() {
            const bsAlert = new bootstrap.Alert(alert);
            bsAlert.close();
        }, 5000);
    });
});

// Confirmation de suppression
function confirmDelete(message) {
    return confirm(message || 'Êtes-vous sûr de vouloir supprimer cet élément ?');
}

// Toggle sidebar sur mobile
document.addEventListener('DOMContentLoaded', function() {
    const sidebarToggle = document.getElementById('sidebarToggle');
    if (sidebarToggle) {
        sidebarToggle.addEventListener('click', function() {
            document.getElementById('sidebarMenu').classList.toggle('show');
        });
    }
});

// Recherche en temps réel (debounce)
let searchTimeout;
function debounceSearch(input, delay = 500) {
    clearTimeout(searchTimeout);
    searchTimeout = setTimeout(function() {
        input.form.submit();
    }, delay);
}

// Sélection multiple dans les tableaux
function toggleSelectAll(source) {
    const checkboxes = document.querySelectorAll('input[type="checkbox"][name="selectedIds"]');
    checkboxes.forEach(function(checkbox) {
        checkbox.checked = source.checked;
    });
}

// Validation de formulaire côté client
(function() {
    'use strict';
    const forms = document.querySelectorAll('.needs-validation');
    Array.from(forms).forEach(function(form) {
        form.addEventListener('submit', function(event) {
            if (!form.checkValidity()) {
                event.preventDefault();
                event.stopPropagation();
            }
            form.classList.add('was-validated');
        }, false);
    });
})();
