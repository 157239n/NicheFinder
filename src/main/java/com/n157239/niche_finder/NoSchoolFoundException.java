package com.n157239.niche_finder;

class NoSchoolFoundException extends RuntimeException {
    NoSchoolFoundException(String schoolName){
        super("Can't find this school: " + schoolName);
    }
}