package net.jammos.realmserver.network

import net.jammos.realmserver.auth.AuthManager
import net.jammos.realmserver.realm.RealmDao
import net.jammos.realmserver.workflow.step.LogonChallengeStep
import net.jammos.utils.network.WorkflowServerHandler

class AuthServerHandler(
        authManager: AuthManager,
        realmDao: RealmDao) : WorkflowServerHandler(
        firstStep = LogonChallengeStep(authManager, realmDao)
)