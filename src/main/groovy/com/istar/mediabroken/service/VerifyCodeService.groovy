package com.istar.mediabroken.service

import com.istar.mediabroken.repo.VerifyCodeRepo
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class VerifyCodeService {
    @Autowired
    VerifyCodeRepo verifyCodeRepo

    Map getVerifyCode(String phoneNumber, String userName) {
        return verifyCodeRepo.getVerifyCode("u:${userName}_p:${phoneNumber}".toString())
    }

    Map getVerifyCode(String phoneNumber) {
        return verifyCodeRepo.getVerifyCode(phoneNumber)
    }

    String sendVerifyCode(String phoneNumber) {
        String verifyCode = generateVerifyCode2()

        verifyCodeRepo.addVerifyCode(phoneNumber, verifyCode)

        verifyCodeRepo.sendVerifyCode(phoneNumber, verifyCode)

        return verifyCode;
    }

    String sendVerifyCode(String phoneNumber, String verifyCode) {
        verifyCodeRepo.sendVerifyCode(phoneNumber, verifyCode)
        return verifyCode;
    }

    void sendLoginMsg(String phoneNumber, String userName, String password) {
        verifyCodeRepo.sendLoginMsg(phoneNumber, userName, password)
    }

    Map generateVerifyCode(String phoneNumber, String userName) {
        def verifyCode = generateVerifyCode2()
        def result = [id: "u:${userName}_p:${phoneNumber}".toString(), verifyCode: verifyCode]
        verifyCodeRepo.addVerifyCode(result.id, verifyCode)
        return result
    }

//    Map generateVerifyCode(String phoneNumber) {
//        String verifyCode = generateVerifyCode2()
//        def result = [id: "p:${phoneNumber}".toString(), verifyCode: verifyCode]
//        verifyCodeRepo.addVerifyCode(result.id, verifyCode)
//    return result
//    }

    private static String generateVerifyCode2() {
        def chars = '123456789'
        def verifyCode = new StringBuffer(4)
        def random = new Random()
        for (int i = 0; i < 4; i++) {
            verifyCode.append(chars.charAt(random.nextInt(chars.length())))
        }
        verifyCode = verifyCode.toString()
        verifyCode
    }

    void removeVerifyCode(String phoneNumber, String userName) {
        verifyCodeRepo.removeVerifyCode("u:${userName}_p:${phoneNumber}")
    }

    void removeVerifyCode(String phoneNumber) {
        verifyCodeRepo.removeVerifyCode("p:${phoneNumber}")
    }
}
