module Demo
{
    sequence<string> StringSeq;
    sequence<byte> img_payload;

    struct BusMsg {
        int busId;
        string textTag;
        string timestamp;
        img_payload image;
    };

    interface Callback{
        void notifyCallback(string msg);
    }
    interface ChatManager{

        void subscribe(Callback* callback);

        StringSeq getState();

        void sendMessage(BusMsg msg);
    }

}